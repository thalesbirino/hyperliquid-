package com.trading.hyperliquid.client;

import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.*;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * EIP-712 Transaction Signer for Hyperliquid DEX
 *
 * Implements Ethereum's EIP-712 typed data signing standard required by Hyperliquid.
 * This class handles cryptographic signing of orders using private keys.
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-712">EIP-712 Specification</a>
 * @see <a href="https://hyperliquid.gitbook.io/">Hyperliquid Documentation</a>
 */
@Slf4j
public class HyperliquidSigner {

    private final ECKeyPair keyPair;
    private final String address;

    /**
     * Chain ID for Hyperliquid
     * Mainnet: 42161 (Arbitrum)
     * Testnet: 421614 (Arbitrum Sepolia)
     */
    private static final long MAINNET_CHAIN_ID = 42161L;
    private static final long TESTNET_CHAIN_ID = 421614L;

    /**
     * EIP-712 Domain Separator constants for Hyperliquid
     */
    private static final String DOMAIN_NAME = "Exchange";
    private static final String DOMAIN_VERSION = "1";

    /**
     * Type hash for EIP-712 domain separator
     */
    private static final byte[] DOMAIN_TYPE_HASH = Hash.sha3(
        "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)".getBytes()
    );

    /**
     * Creates a new signer from a private key
     *
     * @param privateKey Ethereum private key (with or without 0x prefix)
     * @throws IllegalArgumentException if private key is invalid
     */
    public HyperliquidSigner(String privateKey) {
        try {
            // Remove 0x prefix if present
            String cleanKey = privateKey.startsWith("0x")
                ? privateKey.substring(2)
                : privateKey;

            // Validate hex string
            if (!cleanKey.matches("[0-9a-fA-F]{64}")) {
                throw new IllegalArgumentException("Invalid private key format. Must be 64 hex characters.");
            }

            // Create key pair from private key
            BigInteger privateKeyInt = new BigInteger(cleanKey, 16);
            this.keyPair = ECKeyPair.create(privateKeyInt);

            // Derive public address
            this.address = "0x" + Keys.getAddress(keyPair.getPublicKey());

            log.info("Hyperliquid signer initialized for address: {}", address);
        } catch (Exception e) {
            log.error("Failed to initialize signer: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid private key: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the Ethereum address derived from the private key
     *
     * @return Ethereum address with 0x prefix
     */
    public String getAddress() {
        return address;
    }

    /**
     * Signs an order action using EIP-712
     *
     * @param actionHash The hash of the action to sign
     * @param chainId Chain ID (mainnet or testnet)
     * @param verifyingContract Contract address for domain separator
     * @return Signature data containing r, s, v
     * @throws Exception if signing fails
     */
    public Sign.SignatureData signAction(byte[] actionHash, long chainId, String verifyingContract) throws Exception {
        try {
            // Build domain separator
            byte[] domainSeparator = buildDomainSeparator(chainId, verifyingContract);

            // Build final message hash: \x19\x01 + domainSeparator + actionHash
            byte[] message = new byte[2 + domainSeparator.length + actionHash.length];
            message[0] = 0x19;
            message[1] = 0x01;
            System.arraycopy(domainSeparator, 0, message, 2, domainSeparator.length);
            System.arraycopy(actionHash, 0, message, 2 + domainSeparator.length, actionHash.length);

            // Hash the final message
            byte[] messageHash = Hash.sha3(message);

            // Sign with private key
            Sign.SignatureData signature = Sign.signMessage(messageHash, keyPair, false);

            log.debug("Successfully signed action. Message hash: {}", Numeric.toHexString(messageHash));
            return signature;

        } catch (Exception e) {
            log.error("Failed to sign action: {}", e.getMessage());
            throw new Exception("Signing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Builds EIP-712 domain separator
     *
     * @param chainId Chain ID
     * @param verifyingContract Contract address
     * @return Domain separator hash
     */
    private byte[] buildDomainSeparator(long chainId, String verifyingContract) {
        try {
            // Hash domain name
            byte[] nameHash = Hash.sha3(DOMAIN_NAME.getBytes(StandardCharsets.UTF_8));

            // Hash domain version
            byte[] versionHash = Hash.sha3(DOMAIN_VERSION.getBytes(StandardCharsets.UTF_8));

            // Clean contract address (remove 0x prefix)
            String cleanContract = verifyingContract.startsWith("0x")
                ? verifyingContract.substring(2)
                : verifyingContract;

            // Encode domain separator struct
            // domainSeparator = keccak256(abi.encode(
            //     DOMAIN_TYPEHASH,
            //     keccak256(bytes(name)),
            //     keccak256(bytes(version)),
            //     chainId,
            //     verifyingContract
            // ))

            byte[] encoded = new byte[32 * 5];

            // DOMAIN_TYPE_HASH
            System.arraycopy(DOMAIN_TYPE_HASH, 0, encoded, 0, 32);

            // keccak256(name)
            System.arraycopy(nameHash, 0, encoded, 32, 32);

            // keccak256(version)
            System.arraycopy(versionHash, 0, encoded, 64, 32);

            // chainId (uint256)
            byte[] chainIdBytes = Numeric.toBytesPadded(BigInteger.valueOf(chainId), 32);
            System.arraycopy(chainIdBytes, 0, encoded, 96, 32);

            // verifyingContract (address - 20 bytes, left-padded to 32)
            byte[] contractBytes = Numeric.hexStringToByteArray(cleanContract);
            System.arraycopy(contractBytes, 0, encoded, 128 + (32 - contractBytes.length), contractBytes.length);

            return Hash.sha3(encoded);

        } catch (Exception e) {
            log.error("Failed to build domain separator: {}", e.getMessage());
            throw new RuntimeException("Domain separator build failed", e);
        }
    }

    /**
     * Formats signature as hex string for Hyperliquid API
     *
     * @param signature Signature data from Web3j
     * @return Formatted signature object with r, s, v
     */
    public SignatureComponents formatSignature(Sign.SignatureData signature) {
        String r = Numeric.toHexStringNoPrefix(signature.getR());
        String s = Numeric.toHexStringNoPrefix(signature.getS());
        int v = signature.getV()[0];

        return new SignatureComponents(r, s, v);
    }

    /**
     * Helper method to get chain ID based on network type
     *
     * @param isTestnet true for testnet, false for mainnet
     * @return Chain ID
     */
    public static long getChainId(boolean isTestnet) {
        return isTestnet ? TESTNET_CHAIN_ID : MAINNET_CHAIN_ID;
    }

    /**
     * Signature components for Hyperliquid API
     */
    public static class SignatureComponents {
        public final String r;
        public final String s;
        public final int v;

        public SignatureComponents(String r, String s, int v) {
            this.r = r;
            this.s = s;
            this.v = v;
        }

        @Override
        public String toString() {
            return String.format("Signature{r=%s, s=%s, v=%d}",
                r.substring(0, Math.min(10, r.length())) + "...",
                s.substring(0, Math.min(10, s.length())) + "...",
                v);
        }
    }
}
