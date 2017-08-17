/* Copyright (c) 2013-2017 NuoDB, Inc. */

/**
 * @class App.view.HeaderBar
 */
Ext.define('App.view.HeaderBar', {
    extend: 'Ext.container.Container',
    alias: 'widget.headerbar',
    requires: ['App.view.MetricWell'],

    border: false,
    id: 'headerbar',

    layout: {
        type: 'hbox',
        align: 'stretch'
    },

    /** @Override */
    initComponent: function() {
        var me = this;
        var clickHandler = Ext.bind(me.onViewButtonClick, me);
        var changeHandler = Ext.bind(me.onChange, me);

        me.items = [{
            xtype: 'metricwell',
            text: '<b>Workload</b>',
            icon: 'ico-users.png',
            metric: 'workloadStats.all.activeWorkerCount',
            itemId: 'metrics-users',
            input: 'spinner',
            spinnerUpTip: 'Increase simulated users by 2100',
            spinnerDownTip: 'Decrease simulated users by 2100',
            spinnerStopTip: 'Stop all simulated users',
            flex: 0.7,
            listeners: {
                click: clickHandler,
                change: changeHandler
            }
        }, {
            xtype: 'metricwell',
            text: '<b>Transaction Engines</b>',
            icon: 'ico-process.png',
            metric: 'dbStats.usedTeHostCount',
            itemId: 'metrics-hosts',
            inputMaxMetric: 'dbStats.hostCount',
            input: 'spinner',
            spinnerUpTip: 'Add another TE process',
            spinnerDownTip: 'Remove a TE process',
            spinnerStopTip: 'Reset to 1 TE process',
            flex: 0.7,
            href: '/control-panel-processes',
            listeners: {
                click: clickHandler,
                change: changeHandler
            }
        }, {
            xtype: 'metricwell',
            text: '<b>Throughput</b><br />transactions/sec',
            icon: 'ico-dashboard.png',
            format: ',.0',
            displayAvg: true,
            metric: 'transactionStats.all.totalCountDelta',
            itemId: 'metrics-throughput',
            listeners: {
                click: clickHandler
            }
        }, {
            xtype: 'metricwell',
            text: '<b>Avg. Latency</b><br />ms/transaction',
            icon: 'ico-dashboard.png',
            format: ',.0',
            displayAvg: true,
            metric: 'transactionStats.all.avgDurationCalc',
            itemId: 'metrics-latency',
            listeners: {
                click: clickHandler
            }
        }];

        me.callParent(arguments);
        me.btnHosts = me.down('[itemId=metrics-hosts]');
        me.btnRegions = me.down('[itemId=metrics-regions]');
        me.viewButtons = Ext.ComponentQuery.query('button, metricwell', me);

        App.app.on('viewchange', function(viewName) {
            for ( var i = 0; i < me.viewButtons.length; i++) {
                var btn = me.viewButtons[i];
                btn.toggle(btn.getItemId() == viewName || btn.href == viewName, true);
            }
        });
    },

    /** @private event handler */
    onViewButtonClick: function(btnActive) {
        var viewName = btnActive.getItemId();
        App.app.fireEvent('viewchange', viewName, true, null);
    },

    onChange: function(btn, value) {
        var me = this;
        switch (btn.itemId) {
            case 'metrics-users':
                if (!me.adjustUserLoad(value)) {
                    App.app.fireEvent('viewchange', '/control-panel-users', true, 'viewload');
                    App.app.on('viewload', function(viewName) {
                        me.adjustUserLoad(value);
                    }, me, {
                        single: true
                    });
                }
                break;

            case 'metrics-hosts':
                me.adjustHostCount(value);

                break;
            case 'metrics-regions':
                if (btn.activeRequest) {
                    Ext.Ajax.abort(btn.activeRequest);
                }
                btn.noInputSyncUntil = new Date().getTime() + 1000 * 60;
                btn.setWait(true);
                var thisRequest;
                btn.activeRequest = thisRequest = Ext.Ajax.request({
                    url: App.app.apiBaseUrl + '/api/stats/db?numRegions=' + me.btnRegions.getInputValue() + "&numHosts=" + me.btnHosts.getInputValue(),
                    method: 'PUT',
                    params: {
                        tenant: App.app.tenant
                    },
                    scope: this,
                    success: function() {
                        btn.noInputSyncUntil = new Date().getTime() + 1000 * 3;
                    },
                    failure: function(response) {
                        App.app.fireEvent('error', response, null);
                        btn.noInputSyncUntil = 0;
                    },
                    callback: function() {
                        if (btn.activeRequest == thisRequest) {
                            delete btn.activeRequest;
                        }
                    }
                });
                break;

            default:
                break;
        }
    },

    adjustHostCount: function (value) {
        try {
            var url;

            if (value == 0) {
                url = 'resetHostCount';
            } else if (value > 0) {
                url = 'increaseHostCount';
            } else {
                url = 'decreaseHostCount';
            }

            Ext.Ajax.request({
                url: App.app.apiBaseUrl + "/api/processes/" + url,
                method: 'POST',
                scope: this
            });

            return true;
        } catch (e) {
            return false;
        }
    },

    adjustUserLoad: function(value) {
        try {
            var url;
            var alog = $('#activity-log');
            var prefix = (alog.text()) ? "\n" : "";

            if (value == 0) {
                url = 'zeroUserCount';
                alog.append(prefix + "All user workloads have had a stop requested, should show shortly\n");
            } else if (value > 0) {
                url = 'increaseUserCount';
                alog.append(prefix + "An increase in the user workload count has been requested, should appear within 3 minutes\n");
            } else {
                url = 'decreaseUserCount';
                alog.append(prefix + "A decrease in the user workload count has been requested, show show shortly\n");
            }

            document.getElementById('activity-log').scrollTop = document.getElementById('activity-log').scrollHeight;

            Ext.Ajax.request({
                url: App.app.apiBaseUrl + "/api/simulator/" + url,
                method: 'POST',
                scope: this
            });

            return true;
        } catch (e) {
            return false;
        }
    }
});
