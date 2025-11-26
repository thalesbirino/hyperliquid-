#!/usr/bin/env python3
"""
Hyperliquid Order Executor
Receives order data via stdin JSON, executes via SDK, returns result

Usage:
    echo '{"asset": "ETH", "isBuy": true, ...}' | python order_executor.py
"""
import sys
import json
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    stream=sys.stderr
)
logger = logging.getLogger(__name__)

try:
    from hyperliquid.exchange import Exchange
    from hyperliquid.info import Info
    from hyperliquid.utils import constants
except ImportError:
    print(json.dumps({
        "status": "error",
        "message": "hyperliquid-python-sdk not installed. Run: pip install hyperliquid-python-sdk"
    }))
    sys.exit(1)


def execute_order(data: dict) -> dict:
    """
    Execute an order on Hyperliquid via the official SDK

    Args:
        data: Dictionary containing order parameters:
            - asset: Coin name (e.g., "ETH", "BTC")
            - isBuy: True for buy, False for sell
            - size: Order size as string
            - price: Limit price as string
            - reduceOnly: Boolean for reduce-only orders
            - timeInForce: TIF setting ("Gtc", "Ioc", "Alo")
            - isTestnet: True for testnet, False for mainnet
            - hyperliquidAddress: Main account address
            - hyperliquidPrivateKey: Main account private key (if not using API wallet)
            - apiWalletPrivateKey: API wallet private key (optional)

    Returns:
        dict: Hyperliquid API response
    """
    # Determine API URL
    is_testnet = data.get('isTestnet', True)
    base_url = constants.TESTNET_API_URL if is_testnet else constants.MAINNET_API_URL

    logger.info(f"Using {'TESTNET' if is_testnet else 'MAINNET'} API: {base_url}")

    # Configure credentials
    # API Wallet: use api_wallet_private_key but main account address as vault
    api_wallet_key = data.get('apiWalletPrivateKey')

    if api_wallet_key:
        secret_key = api_wallet_key
        # For API wallet, we trade on behalf of the main account
        account_address = data['hyperliquidAddress']
        logger.info(f"Using API Wallet to trade on behalf of: {account_address}")
    else:
        secret_key = data.get('hyperliquidPrivateKey')
        account_address = data['hyperliquidAddress']
        logger.info(f"Using main wallet: {account_address}")

    if not secret_key:
        raise ValueError("No private key provided (neither apiWalletPrivateKey nor hyperliquidPrivateKey)")

    # Clean up private key (remove 0x prefix if present)
    if secret_key.startswith('0x'):
        secret_key = secret_key[2:]

    # Initialize SDK
    logger.info("Initializing Hyperliquid SDK...")

    # Exchange constructor: Exchange(wallet, base_url, vault_address=None, account_address=None)
    # When using API wallet, account_address should be the main account we're trading on behalf of
    # vault_address is only for actual vault trading (multi-user vaults)
    from eth_account import Account
    wallet = Account.from_key(secret_key)

    if api_wallet_key:
        # API wallet: wallet is the API wallet, account_address is the main account
        # Note: use account_address parameter, NOT vault_address
        exchange = Exchange(wallet, base_url, account_address=account_address)
        logger.info(f"Exchange initialized with API wallet, account_address={account_address}")
    else:
        # Main wallet: no vault or account_address needed
        exchange = Exchange(wallet, base_url)
        logger.info(f"Exchange initialized with main wallet")

    # Build order type
    tif = data.get('timeInForce', 'Gtc')
    order_type = {"limit": {"tif": tif}}

    # Extract order parameters
    asset = data['asset']
    is_buy = data['isBuy']
    size = float(data['size'])
    price = float(data['price'])
    reduce_only = data.get('reduceOnly', False)

    logger.info(f"Placing order: {asset} {'BUY' if is_buy else 'SELL'} {size} @ {price} (reduce_only={reduce_only}, tif={tif})")

    # Place order - SDK 0.21+ uses positional args: (name, is_buy, sz, px, order_type, reduce_only)
    result = exchange.order(
        asset,          # coin/name
        is_buy,         # is_buy
        size,           # sz
        price,          # limit_px
        order_type,     # order_type
        reduce_only     # reduce_only
    )

    logger.info(f"Order result: {result}")
    return result


def cancel_order(data: dict) -> dict:
    """
    Cancel an order on Hyperliquid

    Args:
        data: Dictionary containing:
            - asset: Coin name
            - orderId: Order ID to cancel
            - isTestnet: True for testnet, False for mainnet
            - hyperliquidAddress: Main account address
            - apiWalletPrivateKey or hyperliquidPrivateKey

    Returns:
        dict: Hyperliquid API response
    """
    is_testnet = data.get('isTestnet', True)
    base_url = constants.TESTNET_API_URL if is_testnet else constants.MAINNET_API_URL

    api_wallet_key = data.get('apiWalletPrivateKey')
    if api_wallet_key:
        secret_key = api_wallet_key
        account_address = data['hyperliquidAddress']
    else:
        secret_key = data.get('hyperliquidPrivateKey')
        account_address = data['hyperliquidAddress']

    if secret_key.startswith('0x'):
        secret_key = secret_key[2:]

    from eth_account import Account
    wallet = Account.from_key(secret_key)

    if api_wallet_key:
        exchange = Exchange(wallet, base_url, account_address=account_address)
    else:
        exchange = Exchange(wallet, base_url)

    asset = data['asset']
    order_id = data['orderId']

    logger.info(f"Cancelling order {order_id} for {asset}")

    result = exchange.cancel(coin=asset, oid=order_id)

    logger.info(f"Cancel result: {result}")
    return result


def get_positions(data: dict) -> dict:
    """
    Get current positions for an account

    Args:
        data: Dictionary containing:
            - isTestnet: True for testnet, False for mainnet
            - hyperliquidAddress: Account address to query

    Returns:
        dict: Position information
    """
    is_testnet = data.get('isTestnet', True)
    base_url = constants.TESTNET_API_URL if is_testnet else constants.MAINNET_API_URL

    account_address = data['hyperliquidAddress']

    logger.info(f"Getting positions for {account_address}")

    from hyperliquid.info import Info
    info = Info(base_url, skip_ws=True)
    user_state = info.user_state(account_address)

    logger.info(f"User state retrieved")
    return user_state


def main():
    """Main entry point - reads JSON from stdin and executes requested action"""
    try:
        # Read input from stdin
        input_text = sys.stdin.read()
        if not input_text.strip():
            raise ValueError("No input provided via stdin")

        input_data = json.loads(input_text)

        # Determine action
        action = input_data.get('action', 'order')

        if action == 'order':
            result = execute_order(input_data)
        elif action == 'cancel':
            result = cancel_order(input_data)
        elif action == 'positions':
            result = get_positions(input_data)
        else:
            raise ValueError(f"Unknown action: {action}")

        # Output result as JSON
        print(json.dumps(result))

    except json.JSONDecodeError as e:
        error_response = {
            "status": "error",
            "message": f"Invalid JSON input: {str(e)}"
        }
        print(json.dumps(error_response))
        sys.exit(1)

    except Exception as e:
        logger.exception("Error executing action")
        error_response = {
            "status": "error",
            "message": str(e)
        }
        print(json.dumps(error_response))
        sys.exit(1)


if __name__ == '__main__':
    main()
