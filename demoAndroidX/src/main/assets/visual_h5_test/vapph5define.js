

;(function(){
    var defineStore = {
        sdStore : null,
        getDefineTags : function (child){
            var arr = [];
            var DefineTagNum = 0;

            function targetHandle(target,index){
                var tagstore = {
                    level : index,
                    id : 'h' + DefineTagNum
                };
                DefineTagNum ++;
                target.sensorsDefineStore = tagstore;
                arr.push(target);
            }
            //递归便利获取所有可圈选元素的信息 obj
            function func(obj,index){ 
                for(var i=0;i<obj.length;i++){
                    var target = obj[i];
                    var tags = ['input','a','button','textarea'];
                    var tagname = target.tagName.toLowerCase();
                    var parent_ele = target.parentNode;
                    var parent_ele_tagName = parent_ele?parent_ele.tagName ? parent_ele.tagName.toLowerCase() : null:null;
                    var grand_ele = parent_ele.parentNode;
                    var grand_ele_tagName = grand_ele ? grand_ele.tagName ? grand_ele.tagName.toLowerCase() : null:null;
                    
                    if(tags.indexOf(tagname) > -1){
                        targetHandle(target,index);
                    }else if (parent_ele && (parent_ele_tagName === 'button' || parent_ele_tagName === 'a')) {
                        targetHandle(parent_ele,index);
                    }else if(grand_ele && (grand_ele_tagName === 'button' || grand_ele_tagName === 'a')){
                        targetHandle(grand_ele,index);
                    }


        　　　　　　  if(target.children){
                        func(target.children,index+1);
        　　　　　    }
        　　　　 } 
            }    
            func(child,1);
            return arr;

        },
        getVisibility : function(el){
            return (_isVisible(el));    
            function _isVisible(el) {
                var p = el.parentNode;
                
                if ( 9 === p.nodeType ) {
                    return true;
                }
                if (
                        '0' === _getStyle(el, 'opacity') ||
                        'none' === _getStyle(el, 'display') ||
                        'hidden' === _getStyle(el, 'visibility')
                ) {
                    return false;
                }
                var po = el.getBoundingClientRect();
                if ( p ) {
                    if ( ('hidden' === _getStyle(p, 'overflow') || 'scroll' === _getStyle(p, 'overflow')) ) {
                        var parentPo = p.getBoundingClientRect();
                        if (
                            (po.bottom <= parentPo.top)||
                            (po.top >= parentPo.bottom)||
                            (po.right <= parentPo.left)||
                            (po.left >= parentPo.right)
                        ) {  
                            return false;
                        }
                    }
                    return _isVisible(p);
                }
                return true;
            }
        
            function _getStyle(el, property) {
                if ( window.getComputedStyle ) {
                    return document.defaultView.getComputedStyle(el,null)[property];
                }
                if ( el.currentStyle ) {
                    return el.currentStyle[property];
                }
            }
        
            
        },
        //zIndex 取值为 level+zIndex
        getZIndex : function(el){
            var zIndex = window.getComputedStyle(el,null).getPropertyValue("z-index");
            var indexNum = 0;
            if(zIndex && !isNaN(+zIndex)){
                indexNum = +zIndex;
            }
            if(this.sdStore._.isObject(el.sensorsDefineStore)){
                indexNum += el.sensorsDefineStore.level;
            }
            return indexNum;
        },
        //获取可圈选子元素
        getSubElements : function(el){
            var elementsArr = [];
            function testTag(el){
                if(el.children){
                    for(var i=0;i<el.children.length;i++){
                        if(typeof(el.children[i].sensorsDefineStore) == 'object' && el.children[i].sensorsDefineStore.id){
                             elementsArr.push(el.children[i].sensorsDefineStore.id);
                        }
                        testTag(el.children[i]);
                    }
                }
            }
            testTag(el);
            return elementsArr;
        },
        getInfo : function(el){
            var po = el.getBoundingClientRect();
            var tagname = el.tagName;
            var obj = {
                id : el.sensorsDefineStore.id,
                $element_content : this.sdStore._.getElementContent(el,tagname),
                $element_selector : this.sdStore.heatmap.getDomSelector(el),
                tagName : tagname,
                top : po.top,
                left : po.left,
                scrollX : window.pageXOffset,
                scrollY : window.pageYOffset,
                width : po.width,
                height : po.height,
                scale : window.devicePixelRatio,
                visibility : this.getVisibility(el),
                $url : location.href,
                $title : document.title,
                zIndex : this.getZIndex(el),
                subelements : this.getSubElements(el)
            };
            return obj;
        },
        //获取圈选元素信息返回按序排列好的
        getTagsInfo : function(tags){
            var arr = [];
            for(var i=0;i<tags.length;i++){
                arr.push(this.getInfo(tags[i]));
            }
            return this.sortIndex(arr);
        },
        //根据元素层级排序，前端要求 zIndex 为不重复的值
        sortIndex : function(arr){
            arr.sort(function(a,b){
                return a.zIndex - b.zIndex;
            });
            //app 拿到排好序的数组进行依次 +1，不需要 zindex 字段
            for(var i=0;i<arr.length;i++){
                delete arr[i].zIndex;
            }
            return arr;
        },
        //获取元素信息并发送给 app
        getDefineInfo : function(){
            var tags = defineStore.getDefineTags(document.children);
            var tagDataArr = defineStore.getTagsInfo(tags);
            var dataObj = {
                callType : 'visualized_track',
                data : tagDataArr
            };
            console.log(tagDataArr);
            if(typeof window.SensorsData_App_Visual_Bridge === 'object' && window.SensorsData_App_Visual_Bridge.sensorsdata_visualized_mode && window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.sensorsdataNativeTracker){
                window.webkit.messageHandlers.sensorsdataNativeTracker.postMessage(JSON.stringify(dataObj));
            }else if(typeof window.SensorsData_App_Visual_Bridge === 'object' && window.SensorsData_App_Visual_Bridge.sensorsdata_visualized_mode() && window.SensorsData_App_Visual_Bridge.sensorsdata_hover_web_nodes){
                window.SensorsData_App_Visual_Bridge.sensorsdata_hover_web_nodes(JSON.stringify(dataObj));
            } 
        },
        addDefineListener : function(callback){
            var that = this;
            function observe (el, options, callback) {
                var MutationObserver = window.MutationObserver || window.WebKitMutationObserver || window.MozMutationObserver;
                var observer = new MutationObserver(callback);
                observer.observe(el, options);
            }
            var options = {
                childList: true,
                subtree: true,
                attributes: true
            }; 
            
            var changeFunc = (function listener(){
                return defineStore.sdStore._.throttle(callback,1000);
            })();
            observe(document.body, options, changeFunc);
            that.sdStore._.addEvent(window,'scroll',changeFunc);
            that.sdStore._.addEvent(window,'resize',changeFunc);
            that.sdStore._.addEvent(window,'load',changeFunc);
        },
        init : function(){
            var that = this;
            window.sa_jssdk_app_define_mode = function(sd){
                
                that.sdStore = sd;
                var loaded = false;
                window.addEventListener('load',function(){
                    loaded = true;
                    that.getDefineInfo();//获取元素信息
                    that.addDefineListener(that.getDefineInfo);//添加监控器
                });
                setTimeout(function(){
                    if(!loaded){
                        that.getDefineInfo();
                        that.addDefineListener(that.getDefineInfo);
                    }
                },1000);
            };
        }
    };

    defineStore.init();
    


})();