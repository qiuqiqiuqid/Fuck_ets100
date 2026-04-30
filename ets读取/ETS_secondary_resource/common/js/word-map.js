    function getLoad(obj) {
        $(".bgimg").css("width", $(document).width() + "px");
        if (obj.height === 0 || obj.width === 0) {
            console.log("enter getLoad 1 : " + obj.height + " ;; " + obj.width);

            var path = obj.src;
            var index = path.indexOf("?");
            if (index != -1) {
                path = path.substring(0, index);
            }
            console.log("enter getLoad 2 : " + path);
            path = path + "?t=" + Date.parse(new Date());
            $(".bgimg").attr('src', path);
            console.log("enter getLoad 3 : " + path);
        }
        console.log("enter getLoad 4 : " + obj.height + " ;; " + obj.width + " ;; " + obj.src);
    }
    $(function() {
        var isHongkong = GetQueryString('isHongkong');
        var dxApp = new Vue({
            el: "#dxApp",
            data: {
                isHongkong: isHongkong,
                cardType: '',
                isLock: '',
                isNew: 0
            },
            mounted: function() {
                
            },
            updated: function() {

            },
            filters: {},
            computed: {},
            methods: {

            }
        })
        window.dxApp = dxApp;

        // function phonogramShowClock(val) {
        //  console.log(123)
        //  dxApp.isNew = 1;
        //  if (val == 1) {
        //      dxApp.cardType = 'learn';
        //      dxApp.isLock = 1;
        //  } else {
        //      dxApp.cardType = 'use';
        //      dxApp.isLock = 0
        //  }
        // }
        // console.log(dxApp.isNew)
        // 滚动到底部
        var h = $(document).height() - $(window).height();
        setTimeout(function() {
            // $('body').animate({scrollTop: h},500); 
            $('body').scrollTop(h)
        }, 1)

        $(".bgimg").css("width", $(document).width() + "px");


        /*判断系统*/
        var system = 0
        var ua = navigator.userAgent.toLowerCase();
        if (/iphone|ipad|ipod/.test(ua)) {
            /*ios*/
            system = 1
        } else if (/android/.test(ua)) {
            /*android*/
            system = 2
        }
        $(document).on("click", ".PhonogramTest", function(e) {
            e.stopPropagation();
            var id = $(this).attr("id");

            //*********没有锁的isLock为0，有锁的isLock为1
            var imgBlock = $(this).children('.lock-img').css('display');
            if (imgBlock == 'none') {
                dxApp.isLock = 0;
            } else if (imgBlock == 'block') {
                dxApp.isLock = 1
            }
            if (system === 1) {
                /*ios*/
                if (dxApp.isNew == 1) {
                    location.href = "ets://open_symbol_paper?paperId=" + id + "&type=0&isLock=" + dxApp.isLock;
                } else {
                    location.href = "ets://open_symbol_paper?paperId=" + id + "&type=0";
                }
            } else if (system === 2) {
                /*android*/
                if (dxApp.isNew == 1) {
                    window.webInteraction.goWordMap(id, 0, dxApp.isLock);
                } else {
                    window.webInteraction.goWordMap(id, 0);
                }
            }
        })
        $(document).on("click", ".phonogram", function(e) {
            e.stopPropagation();
            //*********没有锁的isLock为0，有锁的isLock为1
            var imgBlock = $(this).children('.lock-img').css('display');
            if (imgBlock == 'none') {
                dxApp.isLock = 0;
            } else if (imgBlock == 'block') {
                dxApp.isLock = 1
            }
            var id = $(this).attr("id");
            if (system === 1) {
                /*ios*/
                if (dxApp.isNew == 1) {
                    location.href = "ets://open_symbol_paper?paperId=" + id + "&type=1&isLock=" + dxApp.isLock;
                } else {
                    location.href = "ets://open_symbol_paper?paperId=" + id + "&type=1";
                }

            } else if (system === 2) {
                /*android*/
                if (dxApp.isNew == 1) {
                    window.webInteraction.goWordMap(id, 1, dxApp.isLock);
                } else {
                    window.webInteraction.goWordMap(id, 1);
                }
            }
        })
    })