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
dojo.provide("aipo.exttimecardsystem");

aipo.exttimecardsystem.onLoadFormDialog = function(portlet_id){

//    var fpicker = dijit.byId("groupnormalselect");
//    if(fpicker){
//        var select = dojo.byId('init_grouplist');
//        var i;
//        var s_o = select.options;
//        if (s_o.length == 1 && s_o[0].value == "") return;
//        for(i = 0 ; i < s_o.length; i ++ ) {
//            fpicker.addOptionSync(s_o[i].value,s_o[i].text,true);
//        }
//    }
//
//    var username = dojo.byId("username");
//    if(username && username.type == 'text'){
//      username.focus();
//    }
}

aipo.exttimecardsystem.onReceiveMessage = function(msg){
    if(!msg) {
        var arrDialog = dijit.byId("modalDialog");
        if(arrDialog){
            arrDialog.hide();
        }
        aipo.portletReload('exttimecardsystem');
    }
    if (dojo.byId('messageDiv')) {
        dojo.byId('messageDiv').innerHTML = msg;
    }
};

aipo.exttimecardsystem.hideDialog = function() {
    var arrDialog = dijit.byId("modalDialog");
    if(arrDialog){
      arrDialog.hide();
    }
    aipo.portletReload('exttimecardsystem');
};

aipo.exttimecardsystem.addHiddenValue = function(form, name, value){
    if (form[name]) {
        form[name].value = value;
    } else {
        var q = document.createElement('input');
        q.type = 'hidden';
        q.name = name;
        q.value = value;
        form.appendChild(q);
    }
}

aipo.exttimecardsystem.onLoadExtTimecardSystemDialog = function(portlet_id){
  var obj = dojo.byId("reason");
  if(obj){
     obj.focus();
  }
}

aipo.exttimecardsystem.changeTypeNew = function(){
    dojo.byId("timecard-type-new").style.display = "";
    dojo.byId("timecard-type-old").style.display = "none";
}

aipo.exttimecardsystem.changeTypeOld = function(){
    dojo.byId("timecard-type-new").style.display = "none";
    dojo.byId("timecard-type-old").style.display = "";
}

aipo.exttimecardsystem.originalHolidayFieldON = function(){
    dojo.byId('originalHolidayField').style.display = "";
}

aipo.exttimecardsystem.originalHolidayFieldOFF = function(){
    dojo.byId('originalHolidayField').style.display = "none";
}