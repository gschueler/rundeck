
databaseChangeLog = {

	changeSet(author: "greg (generated)", id: "1485456335419-1") {
		addColumn(tableName: "auth_token") {
			column(name: "description", type: 'longvarchar')
		}
	}

}
