jenkins-run-notifier-plugin
===========================

Send a HTTP POST request to a configured URI when the run status of a job
changes. The data is sent in JSON format and looks about as follows:

    {
        "job": {
            "uri": "http://jenkins.example.com/job/Test/",
            "name": "Test"
        },
        "run": {
            "uri": "http://jenkins.example.com/job/Test/6/",
            "status": "finalized",
            "duration": 491,
            "name": "#6",
            "buildStatusSummary": "Stable"
        },
        "datetime": "2013-04-13T13:17:19.894Z",
        "busyExecutors": 0,
        "totalExecutors": 2
    }

A request is sent when a job is started, completed or finalized. See
[hudson.model.listeners.RunListener](http://javadoc.jenkins-ci.org/?hudson/model/listeners/RunListener.html)
for information about these events.

Configuration
-------------

- Go to `Manage Jenkins` => `Configure System`
- Configure an URI under `Run Status Notifier`

License
-------

Copyright 2013 Manfred Stock

Released under the MIT license, see
<http://opensource.org/licenses/mit-license.php> for details.
