/* Copyright (c) 2013-2015 NuoDB, Inc. */

/**
 * @class App.controller.RemoteStorefronts
 * 
 * Communicates with the remote Storefront instances to supplement the data stores.
 */
Ext.define('App.controller.RemoteStorefronts', {
    extend: 'Ext.app.Controller',

    /** @Override */
    init: function() {
        var me = this;
        me.callParent(arguments);
        me.appInstanceMap = {}; // uuid-to-AppInstance map
    },

    /** @Override */
    destroy: function() {
        var me = this;
        clearInterval(me.refreshInterval);
        clearInterval(me.instanceRefreshInterval);
        this.callParent(arguments);
    },

    /** @Override */
    onLaunch: function() {
        var me = this;

        // Get a reference to the controller we'll be feeding data
        me.storefrontController = me.application.getController('Storefront');

        // Refresh instance list now and periodically
        me.instanceRefreshInterval = setInterval(Ext.bind(me.onRefreshInstanceList, me), App.app.instanceListRefreshFrequencyMs);
        me.onRefreshInstanceList();

        me.callParent(arguments);
    },

    /** @private interval handler for refreshing app instance list */
    onRefreshInstanceList: function() {
        var me = this;
        Ext.Ajax.request({
            url: App.app.apiBaseUrl + '/api/app-instances',
            method: 'GET',
            params: {
                tenant: App.app.tenant
            },
            scope: this,
            success: function(response) {
                try {
                    me.appInstanceMap = me.buildAppInstanceMap(Ext.decode(response.responseText));
                } catch (e) {
                    return;
                }

                // Delete instances that are no longer alive
                var removeCount = 0;
                var regionStats = me.storefrontController.regionStats;
                for ( var regionName in regionStats) {
                    var region = regionStats[regionName];
                    for ( var uuid in region) {
                        if (!me.appInstanceMap[uuid] || me.appInstanceMap[uuid].region != regionName) {
                            delete region[uuid];
                            delete me.storefrontController.seenInstanceUuidMap[uuid];
                            removeCount++;
                        }
                    }
                }

                // Reset stats if something has changed so our deltas aren't messed up
                if (removeCount > 0) {
                    me.storefrontController.resetStats();
                }
            },
            failure: function(response) {
                me.application.fireEvent('statsfail', response, null);
            }
        });
    },

    buildAppInstanceMap: function(appInstances) {
        var me = this;
        var map = {};
        for ( var i = 0; i < appInstances.length; i++) {
            var appInstance = appInstances[i];
            map[appInstance.uuid] = appInstance;
            var oldInstance = me.appInstanceMap[appInstance.uuid];
            appInstance.outstandingRequestCount = (oldInstance) ? oldInstance.outstandingRequestCount : 0;
        }
        
        return map;
    }
});
