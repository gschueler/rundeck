
- [ ] interceptor order corrected
- [ ] local plugins converted
- [x] metricweb plugin: disabled requirement √


    ~~~
    Caused by: java.lang.NullPointerException: Cannot invoke method withTimer() on null object
        at rundeck.services.FrameworkService.authorizeApplicationResourceSet(FrameworkService.groovy:512)
        at rundeck.services.FrameworkService.projectNames(FrameworkService.groovy:151)
        at rundeck.controllers.MenuController.home(MenuController.groovy:1421)
        ... 14 common frames omitted
    ~~~

- [ ] rundeck.less, bootstrap.less

    ~~~
    com.github.sommeri.less4j.Less4jException: Could not compile less. 350 error(s) occurred:
    ERROR 17:14 The variable "@navbar-default-bg" was not declared.
     16:       background-color: #fff;
     17:       color: @navbar-default-bg;
     18:     }

    ERROR 25:14 The variable "@navbar-inverse-bg" was not declared.
     24:       background-color: #fff;
     25:       color: @navbar-inverse-bg;
     26:     }
     ~~~