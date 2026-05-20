package com.example.wallet.repository;

import com.example.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO wallet (id, balance, created_at, updated_at)
            VALUES (:walletId, :amount, NOW(), NOW())
            ON CONFLICT (id) DO UPDATE
            SET balance = wallet.balance + :amount, updated_at = NOW()
            """, nativeQuery = true)
    int deposit(@Param("walletId") UUID walletId, @Param("amount") long amount);

    @Modifying
    @Query(value = """
            UPDATE wallet
            SET balance = balance - :amount, updated_at = NOW()
            WHERE id = :walletId AND balance >= :amount
            """, nativeQuery = true)
    int withdraw(@Param("walletId") UUID walletId, @Param("amount") long amount);
}
