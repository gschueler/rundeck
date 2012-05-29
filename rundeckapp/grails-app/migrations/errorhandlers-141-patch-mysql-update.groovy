databaseChangeLog = {

	changeSet(author: "greg (generated)", id: "1338428788382-1") {
		addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "auth_token", constraintName: "FK8B5E1CA2F7634DFA", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", referencesUniqueColumn: "false")
	}

	changeSet(author: "greg (generated)", id: "1338428788382-2") {
		addForeignKeyConstraint(baseColumnNames: "error_handler_id", baseTableName: "command_exec", constraintName: "FK74936905329DE323", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "command_exec", referencesUniqueColumn: "false")
	}

	changeSet(author: "greg (generated)", id: "1338428788382-3") {
		addForeignKeyConstraint(baseColumnNames: "scheduled_execution_id", baseTableName: "execution", constraintName: "FKBEF90B18243CF2FF", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "scheduled_execution", referencesUniqueColumn: "false")
	}

	changeSet(author: "greg (generated)", id: "1338428788382-4") {
		addForeignKeyConstraint(baseColumnNames: "workflow_id", baseTableName: "execution", constraintName: "FKBEF90B186C0F497A", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow", referencesUniqueColumn: "false")
	}

	changeSet(author: "greg (generated)", id: "1338428788382-5") {
		addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "node_filter", constraintName: "FK2227B455F7634DFA", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", referencesUniqueColumn: "false")
	}

	changeSet(author: "greg (generated)", id: "1338428788382-6") {
		addForeignKeyConstraint(baseColumnNames: "scheduled_execution_id", baseTableName: "notification", constraintName: "FK237A88EB243CF2FF", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "scheduled_execution", referencesUniqueColumn: "false")
	}

	changeSet(author: "greg (generated)", id: "1338428788382-7") {
		addForeignKeyConstraint(baseColumnNames: "scheduled_execution_id", baseTableName: "rdoption", constraintName: "FKAFFFA927243CF2FF", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "scheduled_execution", referencesUniqueColumn: "false")
	}

	changeSet(author: "greg (generated)", id: "1338428788382-8") {
		addForeignKeyConstraint(baseColumnNames: "option_id", baseTableName: "rdoption_values", constraintName: "FKD63A73AFB2E8DBA", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rdoption", referencesUniqueColumn: "false")
	}

	changeSet(author: "greg (generated)", id: "1338428788382-9") {
		addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "report_filter", constraintName: "FKBD4FF0E3F7634DFA", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", referencesUniqueColumn: "false")
	}

	changeSet(author: "greg (generated)", id: "1338428788382-10") {
		addForeignKeyConstraint(baseColumnNames: "workflow_id", baseTableName: "scheduled_execution", constraintName: "FKB78292066C0F497A", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow", referencesUniqueColumn: "false")
	}

	changeSet(author: "greg (generated)", id: "1338428788382-11") {
		addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "scheduled_execution_filter", constraintName: "FK629503D1F7634DFA", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", referencesUniqueColumn: "false")
	}

	changeSet(author: "greg (generated)", id: "1338428788382-12") {
		addForeignKeyConstraint(baseColumnNames: "command_exec_id", baseTableName: "workflow_command_exec", constraintName: "FK41C5C9851A95C4F1", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "command_exec", referencesUniqueColumn: "false")
	}

	changeSet(author: "greg (generated)", id: "1338428788382-13") {
		dropIndex(indexName: "FK749369056C0F497A", tableName: "command_exec")
	}

	changeSet(author: "greg (generated)", id: "1338428788382-14") {
		dropColumn(columnName: "commands_idx", tableName: "command_exec")
	}

	changeSet(author: "greg (generated)", id: "1338428788382-15") {
		dropColumn(columnName: "workflow_id", tableName: "command_exec")
	}
}
