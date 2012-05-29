databaseChangeLog = {

	changeSet(author: "greg (generated)", id: "1338428196035-1") {
		dropForeignKeyConstraint(baseTableName: "COMMAND_EXEC", baseTableSchemaName: "PUBLIC", constraintName: "FK749369056C0F497A")
	}

	changeSet(author: "greg (generated)", id: "1338428196035-2") {
		dropColumn(columnName: "COMMANDS_IDX", tableName: "COMMAND_EXEC")
	}

	changeSet(author: "greg (generated)", id: "1338428196035-3") {
		dropColumn(columnName: "WORKFLOW_ID", tableName: "COMMAND_EXEC")
	}
}
