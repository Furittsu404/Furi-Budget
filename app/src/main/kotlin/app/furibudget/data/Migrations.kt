package app.furibudget.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    /**
     * v1 to v2: bills, bill payments, bill alerts and templates arrive, and the old recurring transactions
     * become auto-pay bills so nothing the user set up is lost. The CREATE statements are copied from the
     * exported schemas/2.json so Room's schema check passes.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `bills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `type` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, `toAccountId` INTEGER, `categoryId` INTEGER, `repeat` TEXT NOT NULL, `repeatEvery` INTEGER NOT NULL, `anchorDay` INTEGER NOT NULL, `firstDueEpochDay` INTEGER NOT NULL, `endEpochDay` INTEGER, `paymentsTotal` INTEGER, `autoPay` INTEGER NOT NULL, `remindDaysBefore` INTEGER NOT NULL, `note` TEXT NOT NULL, `icon` TEXT NOT NULL, `colorArgb` INTEGER NOT NULL, `active` INTEGER NOT NULL, FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_bills_accountId` ON `bills` (`accountId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_bills_categoryId` ON `bills` (`categoryId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `bill_payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `billId` INTEGER NOT NULL, `dueEpochDay` INTEGER NOT NULL, `status` TEXT NOT NULL, `paidEpochDay` INTEGER NOT NULL, `amountMinor` INTEGER NOT NULL, `transactionId` INTEGER, FOREIGN KEY(`billId`) REFERENCES `bills`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_bill_payments_billId_dueEpochDay` ON `bill_payments` (`billId`, `dueEpochDay`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_bill_payments_transactionId` ON `bill_payments` (`transactionId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `bill_alerts` (`billId` INTEGER NOT NULL, `dueEpochDay` INTEGER NOT NULL, `stage` INTEGER NOT NULL, PRIMARY KEY(`billId`, `dueEpochDay`, `stage`), FOREIGN KEY(`billId`) REFERENCES `bills`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE TABLE IF NOT EXISTS `templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, `categoryId` INTEGER, `note` TEXT NOT NULL, FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_templates_accountId` ON `templates` (`accountId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_templates_categoryId` ON `templates` (`categoryId`)")

            // Old recurring transactions keep their id, their next due date and their paying-itself behaviour.
            db.execSQL(
                """
                INSERT INTO bills (id, name, kind, type, amountMinor, accountId, toAccountId, categoryId, `repeat`, repeatEvery, anchorDay,
                                   firstDueEpochDay, endEpochDay, paymentsTotal, autoPay, remindDaysBefore, note, icon, colorArgb, active)
                SELECT r.id,
                       COALESCE(NULLIF(r.note, ''), (SELECT c.name FROM categories c WHERE c.id = r.categoryId), 'Recurring'),
                       r.kind, 'RECURRING', r.amountMinor, r.accountId, r.toAccountId, r.categoryId, r.`repeat`, 1, r.anchorDay,
                       r.nextDueEpochDay, r.endEpochDay, NULL, 1, -1, '', 'bills',
                       COALESCE((SELECT c.colorArgb FROM categories c WHERE c.id = r.categoryId), -7667713), r.active
                FROM recurrences r
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE IF EXISTS `recurrences`")
        }
    }

    /**
     * v2 to v3: soft delete columns, role and foreign-currency columns on transactions, plus the tables for tags,
     * splits, people, loans, funds and exchange rates. CREATE statements are copied from the exported schemas/3.json.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // New columns. Nullable ones need no default; role gets its default so old rows read as normal transactions.
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `role` TEXT NOT NULL DEFAULT 'NORMAL'")
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `loanId` INTEGER")
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `origAmountMinor` INTEGER")
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `origCurrency` TEXT")
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `rateMicro` INTEGER")
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `deletedAt` INTEGER")
            db.execSQL("ALTER TABLE `bills` ADD COLUMN `deletedAt` INTEGER")
            db.execSQL("ALTER TABLE `goals` ADD COLUMN `deletedAt` INTEGER")
            db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `colorArgb` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `transaction_tags` (`transactionId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, PRIMARY KEY(`transactionId`, `tagId`), FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_tags_tagId` ON `transaction_tags` (`tagId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `transaction_splits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transactionId` INTEGER NOT NULL, `categoryId` INTEGER, `amountMinor` INTEGER NOT NULL, `note` TEXT NOT NULL, FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_splits_transactionId` ON `transaction_splits` (`transactionId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_splits_categoryId` ON `transaction_splits` (`categoryId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `people` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `colorArgb` INTEGER NOT NULL, `note` TEXT NOT NULL, `deletedAt` INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `loans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `personId` INTEGER NOT NULL, `direction` TEXT NOT NULL, `principalMinor` INTEGER NOT NULL, `openedEpochDay` INTEGER NOT NULL, `dueEpochDay` INTEGER, `note` TEXT NOT NULL, `deletedAt` INTEGER, FOREIGN KEY(`personId`) REFERENCES `people`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_loans_personId` ON `loans` (`personId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `funds` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `colorArgb` INTEGER NOT NULL, `targetMinor` INTEGER, `monthlyMinor` INTEGER, `deletedAt` INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `fund_moves` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fundId` INTEGER NOT NULL, `amountMinor` INTEGER NOT NULL, `epochDay` INTEGER NOT NULL, `note` TEXT NOT NULL, `transactionId` INTEGER, `monthKey` TEXT, FOREIGN KEY(`fundId`) REFERENCES `funds`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_fund_moves_fundId_monthKey` ON `fund_moves` (`fundId`, `monthKey`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_fund_moves_transactionId` ON `fund_moves` (`transactionId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `rates` (`code` TEXT NOT NULL, `perBaseMicro` INTEGER NOT NULL, `fetchedAt` INTEGER NOT NULL, `manual` INTEGER NOT NULL, PRIMARY KEY(`code`))")
        }
    }

    /**
     * v3 to v4: a fund belongs to one account, so Home can show what each account holds, has set aside and has free.
     * Funds made before this are put in the first account, which is where the user's money was counted anyway.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `funds` ADD COLUMN `accountId` INTEGER")
            db.execSQL("UPDATE `funds` SET `accountId` = (SELECT `id` FROM `accounts` ORDER BY `sortOrder`, `id` LIMIT 1)")
        }
    }

    /**
     * v4 to v5: Goals and Funds become one thing. Every goal turns into a fund row with its deadline, and the
     * goals table is emptied. A goal that already moved money out of an account (it has GOAL transactions) keeps
     * doing that; an older goal never took money out of anything, so it becomes a set-aside in the first account.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `funds` ADD COLUMN `deadlineEpochDay` INTEGER")
            db.execSQL("ALTER TABLE `funds` ADD COLUMN `holdMode` TEXT NOT NULL DEFAULT 'RESERVE'")
            val firstAccount = db.query("SELECT `id` FROM `accounts` ORDER BY `sortOrder`, `id` LIMIT 1").use { if (it.moveToFirst()) it.getLong(0) else null }
            db.query("SELECT `name`, `icon`, `colorArgb`, `targetMinor`, `savedMinor`, `deadlineEpochDay`, `createdAt`, `deletedAt` FROM `goals` ORDER BY `id`").use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(0)
                    val saved = c.getLong(4)
                    val movedOut = db.query("SELECT COUNT(*) FROM `transactions` WHERE `role` = 'GOAL' AND `note` = ?", arrayOf(name)).use { it.moveToFirst(); it.getInt(0) > 0 }
                    db.execSQL(
                        "INSERT INTO `funds` (`name`, `icon`, `colorArgb`, `targetMinor`, `monthlyMinor`, `deletedAt`, `accountId`, `deadlineEpochDay`, `holdMode`) VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?)",
                        arrayOf<Any?>(name, c.getString(1), c.getInt(2), c.getLong(3), if (c.isNull(7)) null else c.getLong(7), firstAccount, if (c.isNull(5)) null else c.getLong(5), if (movedOut) "MOVE_OUT" else "RESERVE"),
                    )
                    if (saved > 0) {
                        val fundId = db.query("SELECT last_insert_rowid()").use { it.moveToFirst(); it.getLong(0) }
                        val day = (c.getLong(6) / 86_400_000L).coerceAtLeast(0)
                        db.execSQL("INSERT INTO `fund_moves` (`fundId`, `amountMinor`, `epochDay`, `note`, `transactionId`, `monthKey`) VALUES (?, ?, ?, 'From goals', NULL, NULL)", arrayOf<Any?>(fundId, saved, day))
                    }
                }
            }
            db.execSQL("DELETE FROM `goals`")
        }
    }
}
