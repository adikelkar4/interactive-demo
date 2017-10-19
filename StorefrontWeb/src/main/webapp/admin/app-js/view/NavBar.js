/* Copyright (c) 2013-2017 NuoDB, Inc. */

/**
 * @class App.view.NavBar
 */
Ext.define('App.view.NavBar', {
    extend: 'Ext.Component',
    alias: 'widget.navbar',

    border: false,
    id: 'navbar',
    width: 220,
    autoScroll: true,

    links: [{
        title: 'HOME',
        href: '/welcome',
        icon: 'ico-home.png'
    },
    {
        title: 'NuoDB 101',
        href: '/nuodb-101'
    },
    {
        title: 'Guided Tours',
    }, {
        title: 'Scale-Out Performance',
        href: '/tour-scale-out'
    }, {
        title: 'Database Comparison (Coming Soon)',
        href: '/tour-database-comparison'
    }, {
        title: 'Active-Active (Coming Soon)',
        href: '/tour-active-active'
    }, {
        title: 'Continuous Availability (Coming Soon)',
        href: '/tour-continuous-availability'
    }, {
        title: 'Learn More',
    }, {
        title: 'About This Demo',
        href: '/about'
    }, {
        title: 'Resources',
        href: '/resources'
    }, {
        title: 'Download CE',
        href: 'https://www.nuodb.com/product/evaluate-nuodb?utm_source=demo&utm_content=nav'
    }
    ],

    /** @Override */
    initComponent: function() {
        var me = this;

        var html = ['<div id="activity-log-container"><textarea id="activity-log" disabled="disabled"></textarea></div>', '<ul id="nav-links">'];

        for (var i = 0; i < me.links.length; i++) {
            var link = me.links[i];
            html.push('<li');

            if (!link.href) {
                html.push(' class="section-title"');
            }

            html.push('>');

            if (link.href) {
                html.push('<a href="', link.href, '">');
            }

            if (link.icon) {
                html.push('<img src="img/', link.icon, '" width="16" height="16" />');
            } else if (link.prefix) {
                html.push('<span>', link.prefix, '</span>');
            }

            html.push(link.title);

            if (link.href) {
                html.push('</a>');
            }

            html.push('</li>');
        }

        html.push('</ul>');
        me.html = html.join('');
        me.callParent(arguments);

        App.app.on('viewchange', function(href, userInitiated, loadEvent) {
            if (!loadEvent) {
                $('a', me.el.dom).removeClass('active').filter('[href="' + /([^?]*)/.exec(href)[1] + '"]').addClass('active');
            }
        });
    },

    // @Override
    afterRender: function() {
        var me = this;
        me.callParent(arguments);
        $('a', me.el.dom).click(function(e) {
            if ($(this).attr('href').startsWith('http')) {
                window.open($(this).attr('href'), '_blank');

                return false;
            }

            App.app.fireEvent('viewchange', $(this).attr('href'), true, null);

            return false;
        });
    }
});
