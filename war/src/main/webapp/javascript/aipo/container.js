/*
 * Aipo is a groupware program developed by TOWN, Inc.
 * Copyright (C) 2004-2015 TOWN, Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
aipo.PortletLayoutManager = function() {
    shindig.LayoutManager.call(this);
};

aipo.PortletLayoutManager.inherits(shindig.LayoutManager);

aipo.PortletLayoutManager.prototype.getGadgetChrome = function(gadget) {
    var chromeId = 'gadget-chrome-' + gadget.portletId;
    return chromeId ? document.getElementById(chromeId) : null;
};

aipo.PsmlUserPrefStore = function() {
    shindig.UserPrefStore.call(this);
};

aipo.PsmlUserPrefStore.inherits(shindig.UserPrefStore);

aipo.PsmlUserPrefStore.prototype.getPrefs = function(gadget) {
};

aipo.PsmlUserPrefStore.prototype.savePrefs = function(gadget) {

};


aipo.IfrGadget = {
    getMainContent: function(continuation) {
        var iframeId = this.getIframeId();
        gadgets.rpc.setRelayUrl(iframeId, this.serverBase_ + this.rpcRelay);
        gadgets.rpc.setAuthToken(iframeId, this.rpcToken);
        continuation('<div class="' + this.cssClassGadgetContent + '"><iframe id="' +
                iframeId + '" name="' + iframeId + '" class="' + this.cssClassGadget +
                '" src="about:blank' +
                '" frameborder="no"' +
                (this.scrolling ? ' scrolling="' + this.scrolling + '"' : 'no') +
                (this.height ? ' height="' + this.height + '"' : '') +
                (this.width ? ' width="' + this.width + '"' : '') +
                '></iframe></div>');
    },

    finishRender: function(chrome) {
        window.frames[this.getIframeId()].location = this.getIframeUrl();
        //document.getElementById(this.getIframeId()).src = this.getIframeUrl();
    },

    getIframeUrl: function() {
        return this.serverBase_ + 'ifr?' +
                'container=' + this.CONTAINER +
                '&mid=' + this.id +
                '&nocache=' + aipo.container.nocache_ +
                '&country=' + aipo.container.country_ +
                '&lang=' + aipo.container.language_ +
                '&view=' + aipo.container.view_ +
                (this.specVersion ? '&v=' + this.specVersion : '') +
                (shindig.container.parentUrl_ ? '&parent=' + encodeURIComponent(shindig.container.parentUrl_) : '') +
                (this.debug ? '&debug=1' : '') +
                this.getAdditionalParams() +
                this.getUserPrefsParams() +
                (this.secureToken ? '&st=' + this.secureToken : '') +
                '&url=' + encodeURIComponent(this.specUrl) +
                '#rpctoken=' + this.rpcToken +
                (this.viewParams ?
                        '&view-params=' + encodeURIComponent(gadgets.json.stringify(this.viewParams)) : '') +
                (this.hashData ? '&' + this.hashData : '');
    }
};

aipo.IfrGadgetService = function() {
    shindig.IfrGadgetService.call(this);
    gadgets.rpc.register('set_pref', this.setUserPref);
    gadgets.rpc.register('set_title', this.setTitle);
    gadgets.rpc.register('requestNavigateTo', this.requestNavigateTo);
    gadgets.rpc.register('requestCheckActivity', this.requestCheckActivity);
    gadgets.rpc.register('requestCheckTimeline', this.requestCheckTimeline);
    gadgets.rpc.register('requestCheckMessage', this.requestCheckMessage);
    gadgets.rpc.register('requestCheckMessageRead', this.requestCheckMessageRead);
    gadgets.rpc.register('requestCheckMessageDelete', this.requestCheckMessageDelete);
    // gadgets.rpc.register('requestSendMessage', this.requestSendMessage);
};

aipo.IfrGadgetService.inherits(shindig.IfrGadgetService);

aipo.IfrGadgetService.prototype.setUserPref = function(editToken, name, value) {
    var portletId = this.f.replace("remote_iframe_", "").split("_NN_")[0].replace("-popup", "");
    var currentKey = null;
    for (key in aipo.container.gadgets_) {
        var gadget = aipo.container.gadgets_[key];
        if (portletId == gadget.portletId) {
            currentKey = key;
            break;
        }
    }
    var request = {};
    for (var i = 1, j = arguments.length; i < j; i += 2) {
        request[arguments[i]] = arguments[i + 1];
        if (currentKey) {
            aipo.container.gadgets_[currentKey].userPrefs[arguments[i]] = {};
            aipo.container.gadgets_[currentKey].userPrefs[arguments[i]]['value'] = arguments[i + 1];
        }
    }

    var makeRequestParams = {
        "CONTENT_TYPE" : "JSON",
        "METHOD" : "POST",
        "POST_DATA" : gadgets.json.stringify(request)
    };
    var url = "?template=UserPrefUpdateJSONScreen&js_peid=" + encodeURIComponent(portletId);

    gadgets.io.makeNonProxiedRequest(url,
            handleJSONResponse,
            makeRequestParams,
            "application/javascript"
            );

    function handleJSONResponse(obj) {
        if (obj.rc == 200) {
            // success
        }
    }

};

aipo.IfrGadgetService.prototype.setTitle = function(title) {
    // not supported
};

aipo.IfrGadgetService.prototype.requestNavigateTo = function(view, opt_params) {
    var portletId = this.f.replace("remote_iframe_", "").split("_NN_")[0].replace("-popup", "");
    var url = "?js_peid=" + encodeURIComponent(portletId);
    if (view == "canvas") {
        url += '&action=controls.Maximize';
    } else if (view == "home") {
        url += '&action=controls.Restore';
    }
    if (opt_params) {
        var paramStr = gadgets.json.stringify(opt_params);
        if (paramStr.length > 0) {
            url += '&appParams=' + encodeURIComponent(paramStr);
        }
    }
    document.location.href = url;
};

aipo.activityDesktopNotifyEnable = null;
aipo.IfrGadgetService.prototype.requestDesktopNotifyEnable = function(enable) {

    function handleJSONResponse(obj) {
        if (obj.rc == 200) {
            var data = obj.data;
            if(data){
                aipo.activityDesktopNotifyEnable = data.enable;
                aipo.menu.activity.isLoad = false;
            }
        }
    }

    var request = {
    };

    var makeRequestParams = {
        "CONTENT_TYPE" : "JSON",
        "METHOD" : "POST",
        "POST_DATA" : gadgets.json.stringify(request)
    };

    var url = "?template=ActivityNotifyEnableJSONScreen";
    if (aipo.activityDesktopNotifyEnable != null) {
        if (window.webkitNotifications && (!aipo.activityDesktopNotifyEnable || window.webkitNotifications.checkPermission() != 0)) {
        	if(window.webkitNotifications.checkPermission() == 0) {
                url += "&enable=T";
                gadgets.io.makeNonProxiedRequest(url,
                        handleJSONResponse,
                        makeRequestParams,
                        "application/javascript");
        	} else {
        		window.webkitNotifications.requestPermission(function() {
        			if (window.webkitNotifications.checkPermission() == 0) {
        				url += "&enable=T";
        				gadgets.io.makeNonProxiedRequest(url,
        						handleJSONResponse,
        						makeRequestParams,
        					"application/javascript");
        			}
        		});
        	}
        } else  if (window.Notification && (!aipo.activityDesktopNotifyEnable || window.Notification.permission != "granted")) {
        	if(window.Notification.permission == "granted") {
                url += "&enable=T";
                gadgets.io.makeNonProxiedRequest(url,
                        handleJSONResponse,
                        makeRequestParams,
                        "application/javascript");
        	} else {
        		window.Notification.requestPermission(function() {
        			if (window.Notification.permission == "granted") {
        				url += "&enable=T";
        				gadgets.io.makeNonProxiedRequest(url,
        						handleJSONResponse,
        						makeRequestParams,
        						"application/javascript");
        			}
        		});
        	}
        } else {
            url += "&enable=F";
            gadgets.io.makeNonProxiedRequest(url,
                    handleJSONResponse,
                    makeRequestParams,
                    "application/javascript");
        }
    } else {
        gadgets.io.makeNonProxiedRequest(url,
                handleJSONResponse,
                makeRequestParams,
                "application/javascript");

    }
};

aipo.activityMax = null;
aipo.IfrGadgetService.prototype.requestCheckActivity = function(activityId) {
    var request = {
    };

    var makeRequestParams = {
        "CONTENT_TYPE" : "JSON",
        "METHOD" : "POST",
        "POST_DATA" : gadgets.json.stringify(request)
    };

    var url = "?template=CheckActivityJSONScreen&isRead=" + activityId;

    if (aipo.activityMax) {
        url += "&max=" + aipo.activityMax;
    }
    gadgets.io.makeNonProxiedRequest(url,
            handleJSONResponse,
            makeRequestParams,
            "application/javascript"
            );

    function handleJSONResponse(obj) {
        if (obj.rc == 200) {
            var data = obj.data;
            var unreadCount = data.unreadCount;
            var appIdMap = {Workflow:"workflow", todo:"todo", Report:"report", Note:"note"};
            aipo.activityMax = data.max;
            aipo.menu.activity.count(unreadCount);
            aipo.menu.updateTitle();
            for (key in data.activities) {
                var testactivity = data.activities[key];
                var appId = testactivity.appId;
                var group = appIdMap[appId];
                if(group == "workflow" || group == "todo" || group == "report" || group == "note"){
                    aipo.portletReload(group);
                }
            }
            if (aipo.activityDesktopNotifyEnable){
            	if(window.webkitNotifications && window.webkitNotifications.checkPermission() == 0){
            		var popups = new Array();
            		for (key in data.activities) {
            			var activity = data.activities[key];
            			var popup = window.webkitNotifications.createNotification('images/favicon48.png', activity.displayName, activity.text);
            			popup.show();
            			popup.ondisplay = function(event) {
            				setTimeout(function() {
            					event.currentTarget.cancel();
            				}, 7 * 1000);
            			}
            			popups.push(popup);
            		}
            	}else if(window.Notification && window.Notification.permission == "granted"){
            		var popups = new Array();
            		for (key in data.activities) {
            			var activity = data.activities[key];
            			var popup = new window.Notification( activity.displayName, {
                            icon: 'images/favicon48.png',
                            body: activity.text
                            });
            			popup.onshow = function(event) {
            				setTimeout(function() {
            					if(event.currentTarget) {
            						event.currentTarget.close();
            					}
            				}, 7 * 1000);
            			}
            			popups.push(popup);
            		}
            	}
            }
            aipo.menu.activity.isLoad = false;
        }
    }
};

aipo.IfrGadgetService.prototype.requestCheckTimeline = function(params) {
	if(params.senderName != aipo.message.loginUserId){
		dojo.query(".newMessage").style('display', '');
	}
}

aipo.IfrGadgetService.prototype.requestCheckMessage = function(params) {
	if(typeof(params.transactionId) !== 'undefined' && aipo.message.removeTransactionId(params.transactionId)){
		return;
	}
    var notify = true;
    if(typeof(aipo.message.loginUserId) !== 'undefined' && aipo.message.loginUserId==params.userId){
    	notify = false;
    }
    if (aipo.message.isActive && aipo.message.isOpenWindow()
            && aipo.message.currentRoomId && !aipo.message.moreMessageLock) {
        aipo.message.latestMessageList();
        notify = !(params.roomId == aipo.message.currentRoomId);
    } else {
        if(aipo.message.isInit) {
            aipo.message.reloadRoomList();
        } else {
            aipo.message.updateUnreadCount();
        }
    }
    if(notify) {
        var request = {};

        var makeRequestParams = {
                "CONTENT_TYPE" : "JSON",
                "METHOD" : "POST",
                "POST_DATA" : gadgets.json.stringify(request)
        };

        var url = "?template=MessageCheckJSONScreen&messageId=" + params.messageId;

        var handleJSONResponse = function(obj) {
            if (obj.rc == 200) {
                var data = obj.data;
                if (aipo.activityDesktopNotifyEnable && data.displayName) {
                    if (window.webkitNotifications
                            && window.webkitNotifications.checkPermission() == 0) {
                        var popups = new Array();
                        var displayName = data.displayName;
                        var userId = data.userId;
                        var text = data.text;
                        var photoModified = data.photoModified;
                        var icon = 'themes/default/images/common/icon_user100.png';
                        if(data.hasPhoto) {
                            icon = '?template=FileuploadFacePhotoScreen&uid=' + userId + '&t=' + photoModified;
                        }
                        var popup = window.webkitNotifications
                        .createNotification(icon,
                                displayName, text);
                        popup.show();
                        popup.ondisplay = function(event) {
                            setTimeout(function() {
                                event.currentTarget.cancel();
                            }, 7 * 1000);
                        }
                        popups.push(popup);
                    } else if (window.Notification
                            && window.Notification.permission == "granted") {
                        var popups = new Array();
                        var displayName = data.displayName;
                        var userId = data.userId;
                        var text = data.text;
                        var photoModified = data.photoModified;
                        var icon = 'themes/default/images/common/icon_user100.png';
                        if(data.hasPhoto) {
                            icon = '?template=FileuploadFacePhotoScreen&uid=' + userId + '&t=' + photoModified;
                        }
                        var popup = new window.Notification(
                                displayName, {
                                    icon : icon,
                                    body :text
                                });
                        popup.onshow = function(event) {
                            setTimeout(function() {
                                if (event.currentTarget) {
                                    event.currentTarget.close();
                                }
                            }, 7 * 1000);
                        }
                        popups.push(popup);
                    }
                }
            }
        }

        gadgets.io.makeNonProxiedRequest(url, handleJSONResponse,
                makeRequestParams, "application/javascript");
    }
}

aipo.IfrGadgetService.prototype.requestCheckMessageRead = function(params) {
    if (params.roomId) {
        aipo.message.updateReadCount(params.roomId);
    }
}

aipo.IfrGadgetService.prototype.requestCheckMessageDelete = function(params) {
    if (params.roomId) {
    	if(aipo.message.isInit) {
            aipo.message.reloadRoomList();
        } else {
            aipo.message.updateUnreadCount();
        }
        aipo.message.removeNode(params.messageId);
    }
}

aipo.IfrContainer = function() {
    shindig.Container.call(this);
    this.context = new Array();
};

aipo.IfrContainer.inherits(shindig.Container);

aipo.IfrContainer.prototype.gadgetClass = shindig.BaseIfrGadget;

aipo.IfrContainer.prototype.gadgetService = new aipo.IfrGadgetService();

aipo.IfrContainer.prototype.setParentUrl = function(url) {
    if (!url.match(/^http[s]?:\/\//)) {
        url = document.location.href.match(/^[^?#]+\//)[0] + url;
    }

    this.parentUrl_ = url;
};

aipo.IfrContainer.prototype.assign = function(context) {
    this.context.push(context);
};

aipo.IfrContainer.prototype.getContext = function() {
    return this.context;
};

aipo.IfrContainer.prototype.addGadget = function(gadget) {
    this.gadgets_[this.getGadgetKey_(gadget.id)] = gadget;
};

aipo.IfrContainer.prototype.renderGadget = function(gadget) {
    var chrome = this.layoutManager.getGadgetChrome(gadget);
    if (!gadget.count) {
        gadget.count = 0;
    }
    gadget.count++;
    gadget.render(chrome);
};

aipo.IfrContainer.prototype.renderGadgets = function() {
    var context = this.context;
    for (var i = 0; i < context.length; i ++) {
        var c = context[i];
        var gadget = this.createGadget(c);
        gadget.setServerBase(c.serverBase);
        this.addGadget(gadget);
        //this.renderGadget(gadget);
    }

    aipo.cron.start();
};

var tmpGadget;
aipo.IfrContainer.prototype.renderGadgetFromContext = function(context) {
    var gadget = this.createGadget(context);
    gadget.setServerBase(context.serverBase);
    gadget.id = this.getNextGadgetInstanceId();
    gadget.portletId += '-popup';
    var chromeId = 'gadget-chrome-' + gadget.portletId;
    var chrome = chromeId ? document.getElementById(chromeId) : null;
    if (!gadget.count) {
        gadget.count = 0;
    }
    gadget.count++;
    gadget.render(chrome);
    tmpGadget = gadget;
};

shindig.BaseIfrGadget.prototype.getIframeId = function() {
    return this.GADGET_IFRAME_PREFIX_ + this.portletId + '_NN_' + this.count;
};

shindig.BaseIfrGadget.prototype.queryIfrGadgetType_ = function() {
    var gadget = this;
    var subClass = aipo.IfrGadget;
    for (var name in subClass) if (subClass.hasOwnProperty(name)) {
        gadget[name] = subClass[name];
    }
};

shindig.Gadget.prototype.getContent = function(continuation) {
    shindig.callAsyncAndJoin(['getMainContent'], function(results) {
        continuation(results.join(''));
    }, this);
};

aipo.container = new aipo.IfrContainer();
aipo.container.layoutManager = new aipo.PortletLayoutManager();
aipo.container.userPrefStore = new aipo.PsmlUserPrefStore();

aipo.cron = new CronTask(function(decay) {
    var gadgetContext = aipo.container.context;
    var makeRequestParams = {
    		"CONTENT_TYPE" : "JSON",
    		"METHOD" : "POST",
    		"POST_DATA" : gadgets.json.stringify(aipo.container.context)
    };

    var url = "?template=GadgetsSecurityTokenUpdateJSONScreen&view=" + aipo.container.view_;
    if (!aipo.cron.isFirst) {
    	url += "&update=1";
    }

    function handleJSONResponse(obj) {
    	if (obj.rc == 200) {
    		var data = obj.data;
    		for (var i = 0; i < data.length; i++) {
    			var context = data[i];
    			var gadget = aipo.container.gadgets_['gadget_' + context.id];
    			if (!aipo.cron.isFirst) {
    				gadgets.rpc.call('remote_iframe_' + context.portletId + '_NN_' + gadget.count, 'update_security_token', null,
    						context.secureToken);
    				gadget.secureToken = context.secureToken;
    			}
    			var height = context.height;
    			var view = null;
    			if (context.views) {
    				view = context.views[aipo.container.view_];
    				var preferredHeight = 0;
    				if (view) {
    					preferredHeight = view.preferredHeight;
    				} else {
    					var defaultView = context.views['default'];
    					if (defaultView) {
    						preferredHeight = defaultView.preferredHeight;
    					}
    				}
    			}
    			if (height > 0) {
    				gadget.height = height;
    			}
    			if (preferredHeight > 0) {
    				gadget.height = preferredHeight;
    			}
    			gadget.scrolling = context.scrolling ? 'true' : 'no';
    			if (aipo.cron.isFirst) {
    				aipo.container.renderGadget(gadget);
    			}
    		}
    		aipo.cron.isFirst = false;
    		// success
    	}
    }

    gadgets.io.makeNonProxiedRequest(url,
    		handleJSONResponse,
    		makeRequestParams,
    		"application/javascript"
    );
    decay();
}, 30 * 60 * 1000, true);
aipo.cron.isFirst = true;

aipo.container.onPopupGadgets = function() {
    var action = document.getElementById('gadgets-popup-action');
    if (action) {
        location.href = action.href;
    }
};

