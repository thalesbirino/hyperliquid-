package com.trading.hyperliquid.service;

import com.trading.hyperliquid.exception.InvalidStrategyException;
import com.trading.hyperliquid.exception.ResourceNotFoundException;
import com.trading.hyperliquid.mapper.StrategyMapper;
import com.trading.hyperliquid.model.dto.request.StrategyRequest;
import com.trading.hyperliquid.model.entity.Config;
import com.trading.hyperliquid.model.entity.Strategy;
import com.trading.hyperliquid.model.entity.User;
import com.trading.hyperliquid.repository.StrategyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StrategyService focusing on credential validation.
 */
@ExtendWith(MockitoExtension.class)
class StrategyServiceTest {

    @Mock
    private StrategyRepository strategyRepository;

    @Mock
    private ConfigService configService;

    @Mock
    private UserService userService;

    @Mock
    private StrategyMapper strategyMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private StrategyService strategyService;

    private User testUser;
    private Config testConfig;
    private Strategy testStrategy;

    @BeforeEach
    void setUp() {
        strategyService = new StrategyService(
                strategyRepository,
                configService,
                userService,
                strategyMapper,
                passwordEncoder
        );

        // Setup test user
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .hyperliquidAddress("0x1234567890abcdef")
                .active(true)
                .build();

        // Setup test config
        testConfig = Config.builder()
                .id(1L)
                .name("ETH Config")
                .asset("ETH")
                .assetId(1)
                .lotSize(new BigDecimal("0.004"))
                .build();

        // Setup test strategy
        testStrategy = Strategy.builder()
                .id(1L)
                .name("Test Strategy")
                .strategyId("test-strategy-uuid")
                .password("$2a$10$hashedPassword")
                .config(testConfig)
                .user(testUser)
                .active(true)
                .pyramid(false)
                .inverse(false)
                .build();
    }

    // ========================================================================
    // CREDENTIAL VALIDATION TESTS
    // ========================================================================
    @Nested
    @DisplayName("Credential Validation Tests")
    class CredentialValidationTests {

        @Test
        @DisplayName("Should validate credentials successfully")
        void shouldValidateCredentialsSuccessfully() {
            // Given
            when(strategyRepository.findByStrategyId("test-strategy-uuid"))
                    .thenReturn(Optional.of(testStrategy));
            when(passwordEncoder.matches("correct-password", testStrategy.getPassword()))
                    .thenReturn(true);

            // When
            Strategy result = strategyService.validateStrategyCredentials(
                    "test-strategy-uuid",
                    "correct-password"
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStrategyId()).isEqualTo("test-strategy-uuid");
            verify(strategyRepository).findByStrategyId("test-strategy-uuid");
            verify(passwordEncoder).matches("correct-password", testStrategy.getPassword());
        }

        @Test
        @DisplayName("Should throw exception when strategy not found")
        void shouldThrowExceptionWhenStrategyNotFound() {
            // Given
            when(strategyRepository.findByStrategyId("non-existent-uuid"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() ->
                    strategyService.validateStrategyCredentials("non-existent-uuid", "password"))
                    .isInstanceOf(InvalidStrategyException.class)
                    .hasMessageContaining("Invalid strategy ID or password");
        }

        @Test
        @DisplayName("Should throw exception when strategy is inactive")
        void shouldThrowExceptionWhenStrategyInactive() {
            // Given
            testStrategy.setActive(false);
            when(strategyRepository.findByStrategyId("test-strategy-uuid"))
                    .thenReturn(Optional.of(testStrategy));

            // When/Then
            assertThatThrownBy(() ->
                    strategyService.validateStrategyCredentials("test-strategy-uuid", "password"))
                    .isInstanceOf(InvalidStrategyException.class)
                    .hasMessageContaining("Strategy is inactive");
        }

        @Test
        @DisplayName("Should throw exception when password is incorrect")
        void shouldThrowExceptionWhenPasswordIncorrect() {
            // Given
            when(strategyRepository.findByStrategyId("test-strategy-uuid"))
                    .thenReturn(Optional.of(testStrategy));
            when(passwordEncoder.matches("wrong-password", testStrategy.getPassword()))
                    .thenReturn(false);

            // When/Then
            assertThatThrownBy(() ->
                    strategyService.validateStrategyCredentials("test-strategy-uuid", "wrong-password"))
                    .isInstanceOf(InvalidStrategyException.class)
                    .hasMessageContaining("Invalid strategy ID or password");
        }
    }

    // ========================================================================
    // CRUD OPERATION TESTS
    // ========================================================================
    @Nested
    @DisplayName("CRUD Operation Tests")
    class CrudOperationTests {

        @Test
        @DisplayName("Should create new strategy successfully")
        void shouldCreateStrategySuccessfully() {
            // Given
            StrategyRequest request = new StrategyRequest();
            request.setName("New Strategy");
            request.setPassword("password123");
            request.setConfigId(1L);
            request.setUserId(1L);
            // Note: strategyId is null, so existsByStrategyId won't be called

            when(configService.getConfigById(1L)).thenReturn(testConfig);
            when(userService.getUserById(1L)).thenReturn(testUser);
            when(strategyMapper.toEntity(any(), any(), any())).thenReturn(testStrategy);
            when(strategyRepository.save(any())).thenReturn(testStrategy);

            // When
            Strategy result = strategyService.createStrategy(request);

            // Then
            assertThat(result).isNotNull();
            verify(configService).getConfigById(1L);
            verify(userService).getUserById(1L);
            verify(strategyRepository).save(any());
        }

        @Test
        @DisplayName("Should throw exception when strategyId already exists")
        void shouldThrowExceptionWhenStrategyIdExists() {
            // Given
            StrategyRequest request = new StrategyRequest();
            request.setStrategyId("existing-uuid");
            request.setName("New Strategy");
            request.setConfigId(1L);
            request.setUserId(1L);

            when(strategyRepository.existsByStrategyId("existing-uuid")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> strategyService.createStrategy(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Strategy ID already exists");
        }

        @Test
        @DisplayName("Should get strategy by ID")
        void shouldGetStrategyById() {
            // Given
            when(strategyRepository.findById(1L)).thenReturn(Optional.of(testStrategy));

            // When
            Strategy result = strategyService.getStrategyById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw exception when strategy not found by ID")
        void shouldThrowExceptionWhenStrategyNotFoundById() {
            // Given
            when(strategyRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> strategyService.getStrategyById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should get strategy by strategyId (UUID)")
        void shouldGetStrategyByStrategyId() {
            // Given
            when(strategyRepository.findByStrategyId("test-strategy-uuid"))
                    .thenReturn(Optional.of(testStrategy));

            // When
            Strategy result = strategyService.getStrategyByStrategyId("test-strategy-uuid");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStrategyId()).isEqualTo("test-strategy-uuid");
        }

        @Test
        @DisplayName("Should update strategy successfully")
        void shouldUpdateStrategySuccessfully() {
            // Given
            StrategyRequest request = new StrategyRequest();
            request.setName("Updated Strategy");

            when(strategyRepository.findById(1L)).thenReturn(Optional.of(testStrategy));
            when(strategyRepository.save(any())).thenReturn(testStrategy);

            // When
            Strategy result = strategyService.updateStrategy(1L, request);

            // Then
            assertThat(result).isNotNull();
            verify(strategyMapper).updateEntity(any(), any(), any(), any());
            verify(strategyRepository).save(any());
        }

        @Test
        @DisplayName("Should delete strategy")
        void shouldDeleteStrategy() {
            // Given
            when(strategyRepository.findById(1L)).thenReturn(Optional.of(testStrategy));
            doNothing().when(strategyRepository).delete(any());

            // When
            strategyService.deleteStrategy(1L);

            // Then
            verify(strategyRepository).delete(testStrategy);
        }
    }

    // ========================================================================
    // STRATEGY FLAGS TESTS
    // ========================================================================
    @Nested
    @DisplayName("Strategy Flags Tests")
    class StrategyFlagsTests {

        @Test
        @DisplayName("Should validate strategy with pyramid=true")
        void shouldValidateStrategyWithPyramidTrue() {
            // Given
            testStrategy.setPyramid(true);
            when(strategyRepository.findByStrategyId("test-strategy-uuid"))
                    .thenReturn(Optional.of(testStrategy));
            when(passwordEncoder.matches("password", testStrategy.getPassword()))
                    .thenReturn(true);

            // When
            Strategy result = strategyService.validateStrategyCredentials(
                    "test-strategy-uuid",
                    "password"
            );

            // Then
            assertThat(result.getPyramid()).isTrue();
        }

        @Test
        @DisplayName("Should validate strategy with inverse=true")
        void shouldValidateStrategyWithInverseTrue() {
            // Given
            testStrategy.setInverse(true);
            when(strategyRepository.findByStrategyId("test-strategy-uuid"))
                    .thenReturn(Optional.of(testStrategy));
            when(passwordEncoder.matches("password", testStrategy.getPassword()))
                    .thenReturn(true);

            // When
            Strategy result = strategyService.validateStrategyCredentials(
                    "test-strategy-uuid",
                    "password"
            );

            // Then
            assertThat(result.getInverse()).isTrue();
        }

        @Test
        @DisplayName("Should validate strategy with all flags (MODE 4)")
        void shouldValidateStrategyWithAllFlags() {
            // Given
            testStrategy.setPyramid(true);
            testStrategy.setInverse(true);
            when(strategyRepository.findByStrategyId("test-strategy-uuid"))
                    .thenReturn(Optional.of(testStrategy));
            when(passwordEncoder.matches("password", testStrategy.getPassword()))
                    .thenReturn(true);

            // When
            Strategy result = strategyService.validateStrategyCredentials(
                    "test-strategy-uuid",
                    "password"
            );

            // Then
            assertThat(result.getPyramid()).isTrue();
            assertThat(result.getInverse()).isTrue();
        }
    }
}
