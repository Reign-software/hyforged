package reign.software.hyforged.currency.service;

/**
 * Types of currency transactions for audit logging.
 */
public enum TransactionType {
    /** Tradebars earned from loot or rewards */
    EARN,
    
    /** Tradebars spent on services */
    SPEND,
    
    /** Tradebars deposited into a vault */
    VAULT_DEPOSIT,
    
    /** Tradebars withdrawn from a vault */
    VAULT_WITHDRAW,
    
    /** Tradebars earned from selling items */
    SELL,
    
    /** Admin-granted Tradebars */
    ADMIN_GRANT,
    
    /** Passive tree refund cost */
    PASSIVE_REFUND,
    
    /** Vault upgrade cost */
    VAULT_UPGRADE
}
