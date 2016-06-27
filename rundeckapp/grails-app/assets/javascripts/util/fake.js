/*
 *
 * set up global objects for fake exec context for markdeep
 */
(function () {
    window = this;
    window.markdeepOptions = {mode: 'script', detectMath: false};
    window.location = {
        href: ''
    };
    document = {
        createElement: function () {
            return {
                getContext: function () {
                    return {
                        measureText: function () {
                            return {width: 10};
                        }
                    };
                }
            };
        }
    };
    console = {
        log: function () {
        }
    };

    return window;
})();