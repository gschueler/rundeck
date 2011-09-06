
/**
 * ExecutionContext
 */
abstract class ExecutionContext extends BaseNodeFilters{
    String project
    String argString
    String user
    Workflow workflow
    String loglevel="WARN"

    static mapping = {
      user column:'rduser_name'
    }
    Boolean nodeKeepgoing=false
    Boolean doNodedispatch=false
    Integer nodeThreadcount=1
    String adhocRemoteString
    String adhocLocalString
    String adhocFilepath
    Boolean adhocExecution=false
}

