package com.example.wallet.service;

import com.example.wallet.entity.OperationType;
import com.example.wallet.entity.Wallet;
import com.example.wallet.exception.InsufficientFundsException;
import com.example.wallet.exception.WalletNotFoundException;
import com.example.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    private final UUID walletId = UUID.randomUUID();

    @Test
    void deposit_shouldCallRepositoryDeposit() {
        when(walletRepository.deposit(walletId, 500L)).thenReturn(1);

        walletService.processOperation(walletId, OperationType.DEPOSIT, 500L);

        verify(walletRepository).deposit(walletId, 500L);
    }

    @Test
    void withdraw_shouldSucceedWhenFundsSufficient() {
        when(walletRepository.withdraw(walletId, 300L)).thenReturn(1);

        walletService.processOperation(walletId, OperationType.WITHDRAW, 300L);

        verify(walletRepository).withdraw(walletId, 300L);
    }

    @Test
    void withdraw_shouldThrowInsufficientFunds_whenBalanceTooLow() {
        when(walletRepository.withdraw(walletId, 1000L)).thenReturn(0);
        when(walletRepository.existsById(walletId)).thenReturn(true);

        assertThatThrownBy(() -> walletService.processOperation(walletId, OperationType.WITHDRAW, 1000L))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void withdraw_shouldThrowWalletNotFound_whenWalletDoesNotExist() {
        when(walletRepository.withdraw(walletId, 100L)).thenReturn(0);
        when(walletRepository.existsById(walletId)).thenReturn(false);

        assertThatThrownBy(() -> walletService.processOperation(walletId, OperationType.WITHDRAW, 100L))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void getWallet_shouldReturnWallet_whenExists() {
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(1000L);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWallet(walletId);

        assertThat(result.getId()).isEqualTo(walletId);
        assertThat(result.getBalance()).isEqualTo(1000L);
    }

    @Test
    void getWallet_shouldThrowWalletNotFound_whenDoesNotExist() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWallet(walletId))
                .isInstanceOf(WalletNotFoundException.class);
    }
}
