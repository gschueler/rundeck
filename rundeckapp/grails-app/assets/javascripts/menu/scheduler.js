//= require knockout.min
//= require knockout-mapping
//= require ko/binding-url-path-param
//= require ko/binding-message-template
//= require ko/binding-popover
//= require momentutil


function ScheduledJob(data) {
    "use strict";
    var self = this;
    self.project = ko.observable(data.project);
    self.jobId = ko.observable(data.jobId);
    self.jobName = ko.observable(data.jobName);
    self.jobGroup = ko.observable(data.jobGroup);
    self.nextExecution = ko.observable(data.nextExecution);
    self.scheduleEnabled = ko.observable(data.scheduleEnabled || false);
    self.executionEnabled = ko.observable(data.executionEnabled || false);
    self.crontab = ko.observable(data.crontab);
    self.jobTitle = ko.pureComputed(function () {
        var jobGroup = self.jobGroup();
        var jobName = self.jobName();
        return (jobGroup ? jobGroup + '/' : '') + jobName
    });
    self.asMoment = ko.pureComputed(function () {
        return moment(self.nextExecution(), moment.ISO_8601);
    });
    self.nextExecDate = ko.pureComputed(function () {
        return MomentUtil.formatMomentAtDate(self.asMoment());
    });
    self.relativeNextExecDate = ko.pureComputed(function () {
        return self.asMoment().from(moment());
    });
    self.nextExecDateFormat = ko.pureComputed(function () {
        return self.asMoment().format();
    });
}
function SchedulerInfo(data) {
    "use strict";
    var self = this;
    self.url = data.url;
    self.urlOpts = data.urlOpts || {};
    self.scheduledJobs = ko.observableArray([]);
    self.mapping = {
        scheduledJobs: {
            key: function (data) {
                return ko.utils.unwrapObservable(data.jobId);
            },
            create: function (options) {
                "use strict";
                return new ScheduledJob(options.data);
            }
        }
    };
    self.update = function (data) {
        ko.mapping.fromJS(data, self.mapping, self);
    };
    self.update(data);
    self.load = function () {
        jQuery.ajax({
            url: _genUrl(self.url, self.urlOpts),
            dataType: 'json',
            method: 'GET',
            success: function (data) {
                self.update(data);
            }
        });
    }
}
