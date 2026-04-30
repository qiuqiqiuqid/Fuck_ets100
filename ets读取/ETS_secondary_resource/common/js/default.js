var god = GetQueryString('god')||0;
var ets_grade = {
    // 测试小学
    89712: '一年级',
    89713: '二年级',
    89714: '三年级',
    89715: '四年级',
    89716: '五年级',
    89717: '六年级',
    // 正式小学

    90325: '二年级',
    90326: '三年级',
    90327: '四年级',
    90328: '五年级',
    90329: '六年级',
    // 初高中
    5258: '七年级',
    5259: '八年级',
    5260: '九年级',
    5263: '高一',
    5264: '高二',
    5265: '高三',
}
var openTypeTextArr = ['listenOpen', 'speakOpen', 'readOpen', 'writeOpen', 'viewOpen'];
var tabLabelKeyArr = ['listen', 'speak', 'read', 'write', 'view'];
var ua = navigator.userAgent.toLowerCase();
// 接口全局域名
var API;
if (ua.indexOf("domain") != -1) {
    var subUa = ua.substring(ua.indexOf("domain"));
    API = subUa.split("==")[1]
}
sessionStorage.setItem('url', '');
/*获取url参数 str带引号*/
function GetQueryString(str) {
    var href = String(window.document.location.href);
    var rs = new RegExp("([\?&])(" + str + ")=([^&]*)(&|$)", "gi").exec(href);
    if (rs) {
        return decodeURI(rs[3]);
    } else {
        return '';
    }
}
var browser = {
    versions: function() {
        var u = navigator.userAgent;
        return { //移动终端浏览器版本信息
            trident: u.indexOf('Trident') > -1, //IE内核
            presto: u.indexOf('Presto') > -1, //opera内核
            webKit: u.indexOf('AppleWebKit') > -1, //苹果、谷歌内核
            gecko: u.indexOf('Gecko') > -1 && u.indexOf('KHTML') === -1, //火狐内核
            mobile: !!u.match(/AppleWebKit.*Mobile.*/) || !!u.match(/AppleWebKit/), //是否为移动终端
            ios: !!u.match(/\(i[^;]+;( U;)? CPU.+Mac OS X/), //ios终端
            android: u.indexOf('Android') > -1, //android终端或者uc浏览器
            iPhone: u.indexOf('iPhone') > -1, //是否为iPhone或者QQHD浏览器
            iPad: u.indexOf('iPad') > -1, //是否iPad
            webApp: u.indexOf('Safari') === -1, //是否web应该程序，没有头部与底部
            weixinApp: u.indexOf('MicroMessenger') > -1 //是否微信APP
        };
    }(),
    language: (navigator.browserLanguage || navigator.language).toLowerCase()
};
// 设置top颜色
function setTopColor(color) {
    // try {
    //     var ua = navigator.userAgent.toLowerCase();
    //     if (/ets100\.ios\.app/.test(ua)) {
    //         /*ios*/
    //         //sdsdsds
    //         location.href = "ets://set_ecard_detail?bgColor=" + color;
    //     } else if (/ets100\.android\.app/.test(ua)) {
    //         /*android*/
    //         window.webInteract.setTopColor(color);
    //     }
    // } catch (err) {
    //     console.log(err);
    // }
}
// 设置页面返回时的状态
function setBackButtonStyle(bl) {
    try {
        // alert(bl)
        var ua = navigator.userAgent.toLowerCase();
        if (/ets100\.ios\.app/.test(ua)) {
            /*ios*/
            location.href = "ets://set_back_button_style?style=" + bl;
        } else if (/ets100\.android\.app/.test(ua)) {
            /*android*/
            if(window.webInteract && window.webInteract.setBackButtonStyle){
                window.webInteract.setBackButtonStyle(bl);
            }
        }
    } catch (err) {
        console.log(err);
    }
}
// 时间栏跟标题栏的高度
// appname/ets100.ios.app|version==4.0.0|
// var statusHeight=20;
// var titleHeight=44;
function getHeight() {
    try {
        var ua = navigator.userAgent.toLowerCase();
        var splitQueryArr = ua.split('|');
        statusHeight = splitQueryArr[2].split('==')[1];
        titleHeight = splitQueryArr[3].split('==')[1];

    } catch (err) {
        console.log(err);
    }
}
// 是否是ios手机
function isIos() {
    try {
        var ua = navigator.userAgent.toLowerCase();
        if (/ets100\.ios\.app/.test(ua)) {
            /*ios*/
            return true
            // location.href = "ets://set_ecard_detail?bgColor=" + color;
        } else if (/ets100\.android\.app/.test(ua)) {
            /*android*/
            return false
            // window.webInteract.setTopColor(color);
        }
    } catch (err) {
        console.log(err);
    }
}
//显示提示
function showTips(str, time, callback) {
    var t = time || 3000;
    if ($('.tips_con').length == 0) {
        var div = $("<div>").addClass("tips_con");
        $('body').append(div);
    };
    $(".tips_con").text(str);
    var width = $('.tips_con').innerWidth() / 2;
    $('.tips_con').css("margin-left", -width + 'px').show();
    setTimeout(function() {
        $('.tips_con').text('').hide();
        if (callback) {
            callback()
        };
    }, t)
}
// 检测是否下架
function idCheckPro(product_id, callback) {
    $.post('/product/check-available', {
        product_id: product_id
    }, function(data) {
        callback(data);
    }, 'json').error(function(xhr, statu) {
        console.log(xhr)
        var data = JSON.parse(xhr.responseText);
        $('#submit').removeAttr('disabled');
        showTips(data.message);
    })
}
// closeWebView
function closeWebView() {
    try {
        var ua = navigator.userAgent.toLowerCase();
        if (/iphone|ipad|ipod/.test(ua)) {
            /*ios*/
            // location.href = "ets://set_ecard_detail?bgColor="+color;
            location.href = "ets://pop_back?pop=true"
        } else if (/android/.test(ua)) {
            /*android*/
            window.webInteract.closeWebView();
        }
    } catch (err) {
        console.log(err);
    }
}

function goBackUrl(num) {
    var path = window.location.pathname;
    if (path === '/mobile/cards.html') {
        closeWebView()
    } else if (typeof popBack === 'function') {
        popBack();
    } else if (path === '/mobile/cards-detail.html') {
        location.href = 'cards.html';
    } else {
        var type = GetQueryString("type");
        if (type == '1') {
            closeWebView();
        } else {
            window.history.back();
        }
    }
}

function returnToCurrentPage11() {
    var res = 0
    if (typeof popBack === 'function') {
        if (VM.selectStatu == '' && VM.choosedStatu == 0) {} else {
            goBackUrl();
            res = 1
        }
    }
    return res
}

function returnToCurrentPage() {
    var res = 0;
    var urlPath = window.location.pathname;
    var ua = navigator.userAgent.toLowerCase();
    if (/iphone|ipad|ipod/.test(ua)) {
        if (/setting\.html|common-problem-list\.html|common-problem-detail\.html/.test(urlPath)) {
            window.history.go(-1);
            res = 1
        } else {
            if ($('.child-tag.active').length > 0) {
                closeTagView()
                res = 1
            }
            return res
        }
    } else if (/android/.test(ua)) {
        if (/setting\.html|common-problem-list\.html|common-problem-detail\.html/.test(urlPath)) {
            window.history.back();
        } else {
            /*android*/
            if ($('.child-tag.active').length > 0) {
                closeTagView()
            } else {
                window.webInteract.closeWebView();
            }
            // window.webInteract.closeWebView();
        }
    }
}

function closeTagView() {
    if(VM.tagDemoText){
        return
    }
    var l = $('.child-tag.active').length;
    if (l == 1) {
        VM.growingLineHidden = false;
        $('.growth-tab').show();
    }
    VM.tagsArrForname.pop();
    VM.nowParentTagId.pop();
    VM.childItems.pop();
    $('.child-tag.active').eq(l - 1).removeClass('active');
}

function addClass() {
    try {
        var ua = navigator.userAgent.toLowerCase();
        if (/iphone|ipad|ipod/.test(ua)) {
            /*ios*/
            // location.href = "ets://set_ecard_detail?bgColor="+color;
            location.href = "ets://add_class?add=1"
        } else if (/android/.test(ua)) {
            /*android*/
            window.webInteract.addTeacher();
        }
    } catch (err) {
        console.log(err);
    }
}

function resetPayBtnStatus() {
    $("button.submit_btn").removeAttr('disabled');
}
//  重置button
function resetButton(bool) {
    if (bool) {
        $('#submit').removeAttr('disabled');
    } else {
        $('#submit').attr('disabled', 'disabled');
    };
}

// 从客户端取出参数 
function preScorePara(para, page) {
    para = JSON.parse(para);
    window.account_id = para.ecard_id;
    window.class_id = para.class_id;
    window.resource_id = para.res_id;
    window.token = para.token;
    if (para.isCheck == 0 || para.isCheck == 1) {
        window.isCheck = para.isCheck;
    }
    if (para.score || para.score == "") {
        window.score = Math.round(para.score);
    }
    if (para.totalScore) {
        window.totalScore = para.totalScore;
    }
    window.name = para.name;
    if (para.portrait_cache) {
        window.portrait_cache = para.portrait_cache;
    } else {
        window.portrait_cache = "http://cdn.fei.ets100.com/portrait/FnkETnwehH-1518081077183.png"
    }

    window.school = para.school;
    window.resRank = para.resRank;

    if (page == 'detail') {
        load()
    } else if (page == 'rank') {
        loadRank()
    } else if (page == 'learning') {
        loadLearning()
    }

    // window.inRanking=para.inRanking
}

function load() {
    $.ajax({
        url: API + '/point-service/account/info',
        type: 'post',
        dataType: 'json',
        data: {
            token: token,
            id: account_id,
            resource_id: resource_id,
        },
        success: function(data) {
            if (data.code == 0) {
                var dps = document.documentElement.clientWidth / 375;
                // dp = 1;
                var VM = new Vue({
                    el: '#app',
                    data: {
                        tabIndex: 0,
                        contentChart: 0,
                        distributionName: '班级',
                        tabLeft: '',
                        tabWidth: '',
                        // 全班最高分
                        highestScore: '',
                        // 资源最高分
                        highestResScore: '',
                        selfRank: '--',
                        statisticsLoc: '',
                        class_id: class_id,
                        isShowLoading: true,
                        loadingFlag: '',
                        latePdate: data.data.pdate,
                        tabRes: '',
                        tabResAdd: "",
                        // 是否显示资源排名轨迹
                        isShowResRank: "",
                        tabBarWidth: ""

                    },
                    methods: {
                        tabAddress: function(e) {},
                        goHelp: function() {
                            setBackButtonStyle(1);
                            window.location.href = './common-problem-list.html?statusHeight=' + statusHeight + "&titleHeight=" + titleHeight
                        },
                        // 分布图
                        distributionCharts: function(tabName) {
                            var that = this;
                            if (tabName == 'class') {
                                var url = API + '/point-service/class/distribution'
                                var disData = {
                                    token: token,
                                    id: class_id
                                }
                            } else {
                                var url = API + '/point-service/resource/distribution';
                                var disData = {
                                    token: token,
                                    id: resource_id
                                }
                            }
                            $.ajax({
                                url: url,
                                type: 'post',
                                dataType: 'json',
                                data: disData,
                                success: function(chartData) {
                                    if (chartData.data) {
                                        var maxData = 0;
                                        chartData.data.forEach(function(item, index) {
                                            if (item.predict_score > maxData) {
                                                maxData = item.predict_score
                                            }
                                        })

                                        // 班级
                                        if (tabName == 'class') {
                                            that.highestScore = maxData;
                                            that.statisticsLoc = chartData.ext.grade_name + chartData.ext.class_name + '班'
                                        } else {
                                            // 省市文案
                                            if (chartData.ext.city) {
                                                that.tabRes = chartData.ext.city;
                                                that.tabResAdd = "市"
                                            } else {
                                                that.tabRes = chartData.ext.province
                                                that.tabResAdd = "省"
                                            }
                                            that.highestResScore = maxData;
                                            // that.distributionName = chartData.ext.province + chartData.ext.city;
                                            if (chartData.ext.city) {
                                                that.distributionName = chartData.ext.city
                                                that.statisticsLoc = chartData.ext.city
                                            } else {
                                                that.distributionName = chartData.ext.province
                                                that.statisticsLoc = chartData.ext.province
                                            }
                                        }

                                        // 分布图的数据[[人数，分数]]
                                        var distributionDatas = [];
                                        var maxAccount = 0;
                                        chartData.data = chartData.data.sort(compare('predict_score'))
                                        chartData.data.forEach(function(item, index) {
                                            var singleData = [];
                                            singleData.push(item.account_num);
                                            singleData.push(item.predict_score)
                                            distributionDatas.push(singleData);
                                            if (item.account_num > maxAccount) {
                                                maxAccount = item.account_num;
                                            }
                                        })
                                        var dom4 = document.getElementById('spread-sccore-container');
                                        var selfLoc;
                                        if (that.scores) {
                                            selfLoc = distributionDatas.filter(function(item, index) {
                                                return item[1] == VM.scores.total_score;

                                            })
                                            if (selfLoc.length == 0) {
                                                selfLoc = [
                                                    ['', '']
                                                ]
                                            }
                                        } else {
                                            selfLoc = [
                                                ['', '']
                                            ]
                                        }
                                        distributionDatas.unshift([0, 150])
                                        distributionDatas.push([0, 0])
                                        // distributionChart(dom4, distributionDatas, selfLoc[0], maxAccount)
                                    }
                                }
                            })
                        },
                        // 成长轨迹//有分数的时候
                        selfGrow: function() {
                            $.ajax({
                                url: API + '/point-service/account/trail',
                                type: 'post',
                                dataType: 'json',
                                data: {
                                    id: account_id,
                                    resource_id: resource_id,
                                    token: token,
                                    size: 180
                                },
                                success: function(chartData) {
                                    // 当前学生所有的预测分
                                    var predict_scores = [];
                                    // 当前学习所有的有预测分的日期
                                    var pdates = [];
                                    // 当前学生的所有班级排名
                                    var class_ranks = [];
                                    // 当前学生的所有省级排名
                                    var resource_ranks = [];
                                    // 最近一个日期
                                    var latestDate = '';
                                    chartData.data.forEach(function(item, index) {
                                        predict_scores.unshift(item.predict_score)
                                        pdates.unshift(item.pdate.substring(5))
                                        if (item.class_rank > 0) {
                                            class_ranks.unshift(item.class_rank)
                                        }

                                        resource_ranks.unshift(item.resource_rank)
                                        if (item.pdate > latestDate) {
                                            latestDate = item.pdate;
                                        }
                                    })

                                    // if (pdates.length % 2 == 1) {
                                    //     pdates = [{ value: pdates[0], textStyle: { padding: [0, 0, 0, 30 * dps] } }, pdates[(pdates.length - 1) / 2], { value: pdates[pdates.length - 1], textStyle: { padding: [0, 40 * dps, 0, 0] } }]
                                    // } else {
                                    //     pdates = [{ value: pdates[0], textStyle: { padding: [0, 0, 0, 30 * dps] } }, pdates[(pdates.length - 2) / 2], { value: pdates[pdates.length - 1], textStyle: { padding: [0, 40 * dps, 0, 0] } }]
                                    // }
                                    // 最近最新的个人数据
                                    var latestData = chartData.data.filter(function(item) {
                                        return item.pdate == latestDate
                                    })
                                    if (latestData[0].class_rank > 0) {
                                        VM.selfRank = latestData[0].class_rank;
                                    } else {
                                        VM.selfRank = '--'
                                    }
                                    // VM.selfRank = latestData[0].class_rank;

                                    // 预测分图表
                                    var dom = document.getElementById('growing-container');
                                    // 班级排名container
                                    var dom1 = document.getElementById('growing-container1');
                                    var firstDate = pdates[0];
                                    var lastDate = pdates[pdates.length - 1]
                                    pdates.forEach(function(item, index) {
                                        if (index == 0) {
                                            pdates[0] = {}
                                            pdates[0].value = firstDate;
                                            pdates[0].textStyle = {}
                                            pdates[0].textStyle.padding = [0, 0, 0, 30 * dps]
                                        } else if (index == pdates.length - 1) {
                                            pdates[index] = {}
                                            pdates[index].value = lastDate;
                                            pdates[index].textStyle = {}
                                            pdates[index].textStyle.padding = [0, 40 * dps, 0, 0]
                                        }
                                    })
                                    growChart(dom, pdates, predict_scores, '成绩', VM.scores.total_point)
                                    // 省级排名container
                                    if (class_id != -1) {

                                        growChart(dom1, pdates, class_ranks, '排名');
                                    }
                                    // 班级，省级排名tab
                                    $('.tab .title').on('click', function(e) {
                                        if (e.target.dataset.index == 0) {
                                            // 班级排名
                                            VM.distributionName = '班级';
                                            if (latestData[0].class_rank > 0) {
                                                VM.selfRank = latestData[0].class_rank;
                                            } else {
                                                VM.selfRank = '--'
                                            }
                                            // VM.selfRank = latestData[0].class_rank;
                                        } else if (e.target.dataset.index == 1) {
                                            // 省市排名
                                            // latestData[0].predict_score = 121
                                            if (latestData[0].predict_score * 100 / data.ext.total_point >= 0.6) {
                                                VM.isShowResRank = true;
                                                setTimeout(function() {
                                                    if (latestData[0].resource_rank > 0) {
                                                        VM.selfRank = latestData[0].resource_rank;
                                                    } else {
                                                        VM.selfRank = '--'
                                                    }

                                                    var dom2 = document.getElementById('growing-container-demo');
                                                    $('#growing-container-demo').width($('.tab').width())
                                                    growChart(dom2, pdates, resource_ranks, '排名')
                                                }, 0)

                                            } else {
                                                VM.isShowResRank = false;
                                                VM.selfRank = '--'
                                            }

                                        }
                                    })
                                    VM.loadingFlag = true;
                                },
                                error: function() {

                                }
                            })
                        },
                        addClass: function() {

                            window.addClass()
                        },
                        goDetailHelp: function() {
                            setBackButtonStyle(1);
                            window.location.href = './common-problem-detail.html?title=如何进入排名&statusHeight=' + statusHeight + "&titleHeight=" + titleHeight
                        }
                    },
                    beforeCreate: function() {
                        // console.log('beforeCreate')
                    },
                    mounted: function() {
                        var that = this;
                        setTimeout(function() {
                            /*********预测进程图
                             ********startColor:占比为0-50的开始颜色
                             ********midColor:占比为0-50的结束颜色，50-100的开始颜色
                             ********midwayColor:占比为50-100的结束颜色
                             ********endColor:占比为50-100的空白颜色
                             ********rightPrecentage:预测分占总分的比例,位于右侧，比例少于50
                             ********leftPrecentage：预测分占总分的比例，位于左侧，比例高出50%的范围
                             ********scale:预测分占总分的比例
                             */
                            // that.scores.total_score = 0
                            if (that.scores.total_score >= 0) {
                                scale = that.scores.total_score / that.scores.total_point;
                            }
                            if (scale >= 0) {
                                if (scale < 0.5) {
                                    rightPrecentage = scale;
                                    leftPrecentage = 0;
                                } else {
                                    rightPrecentage = 0.5;
                                    leftPrecentage = scale - rightPrecentage;
                                }
                            }
                            var canvas = document.getElementById('process-canvas');
                            var ctx = canvas.getContext('2d');
                            canvas.width = canvas.width * dps
                            canvas.height = canvas.height * dps
                            var x = canvas.width / 8;
                            var y = canvas.height / 8;
                            ctx.scale(4, 4);
                            // ctx.lineWidth = 5 * dps;
                            // 灰色内圆
                            ctx.beginPath();
                            ctx.arc(x, y, 69 * dps, 0, 2 * Math.PI, false);
                            ctx.strokeStyle = '#e6e6e6';
                            ctx.closePath();
                            ctx.stroke();
                            // ctx.moveTo(170, 120);
                            var grd = ctx.createLinearGradient(0, (x + 77), 0, 0);
                            grd.addColorStop(0, "#FEBB48");
                            grd.addColorStop(1, "#FB7A7F");
                            var grd1 = ctx.createLinearGradient(0, (x + 77), 0, 0);
                            grd1.addColorStop(0, "#FEBB48");
                            grd1.addColorStop(1, "#2DF493");
                            // 灰色圆
                            ctx.beginPath();
                            ctx.arc(x, y, 77 * dps, 3 * Math.PI / 2, -Math.PI / 2, false);
                            if (ctx.setLineDash) {
                                ctx.setLineDash([2, 2, 2, 2]);
                            }
                            ctx.lineWidth = 8 * dps;
                            ctx.strokeStyle = '#e9e9e9';
                            ctx.stroke();
                            if (rightPrecentage) {
                                var startAngle = 3 * Math.PI / 2;
                                var endAngle = (3 * Math.PI / 2) + (rightPrecentage * 2 * Math.PI);
                                ctx.beginPath();
                                if (ctx.setLineDash) {
                                    ctx.setLineDash([2, 2, 2, 2]);
                                }
                                ctx.strokeStyle = grd;
                                ctx.arc(x, y, 77 * dps, startAngle, endAngle, false);
                                ctx.stroke(); // 画圆
                            }
                            if (leftPrecentage) {
                                var startAngle = Math.PI / 2;
                                var endAngle = (Math.PI / 2) + (leftPrecentage * 2 * Math.PI);
                                ctx.beginPath();
                                if (ctx.setLineDash) {
                                    ctx.setLineDash([2, 2, 2, 2]);
                                }
                                ctx.strokeStyle = grd1;
                                ctx.arc(x, y, 77 * dps, startAngle, endAngle, false);
                                ctx.stroke();
                            }

                        }, 0)
                        this.distributionCharts('class')
                        this.distributionCharts('resource')
                        if (this.scores) {
                            this.selfGrow()
                        } else {
                            this.loadingFlag = true
                        }
                        $('#app1').removeClass('hidePreScore');
                        var isIos = window.isIos();
                        var iosDp = $('html').attr('data-dpr')
                        if (isIos) {
                            $('.small-div .no-prescore').css('fontSize', "0.64rem")
                            $('.small-div .score').css('fontSize', "0.96rem")
                            $('.small-div .total-score').css('fontSize', "0.373rem");
                            $('.out-on-off').css('fontSize', 18 * iosDp + 'px');
                        } else {
                            $('.out-on-off').css('fontSize', '16px');
                        }
                        $(".time-status-bar").height(statusHeight)
                        $(".head-operate-container").height(titleHeight);
                        $('.scroll-container').css('padding-top', $('.all-bar').height())
                        this.tabWidth = $('.title .text').width();
                        this.tabLeft = $('.tab .title').width() / 2 - $('.tabs-active-bar').width() / 2;
                        this.tabBarWidth = $('.tabs-active-bar').width()
                    },
                    created: function() {

                    },
                    updated: function() {
                        // console.log('这里呢')
                    },
                    computed: {
                        tabBarStyle: function() {
                            return {
                                '-webkit-transform': 'translateX(' + this.tabLeft + 'px)',
                                '-ms-transform': 'translateX(' + this.tabLeft + 'px)',
                                '-moz-transform': 'translateX(' + this.tabLeft + 'px)',
                                '-o-transform': 'translateX(' + this.tabLeft + 'px)',
                                transform: 'translateX(' + this.tabLeft + 'px)',
                                // width: this.tabWidth + 'px',
                            }
                        },
                        scores: function() {
                            if (data.data) {
                                return {
                                    // total_score: 0,
                                    total_score: data.data.predict_score,
                                    total_point: data.ext.total_point / 100,
                                }
                            } else {
                                return ''
                            }
                        }

                    },
                    watch: {
                        loadingFlag: function(newVal, oldVal) {
                            if (newVal) {
                                this.isShowLoading = false;
                            }
                        }
                    }
                })
                window.VM = VM;
                // tab取消分布图
                $(document).on('click', '.tab .title', function(e) {
                    VM.contentChart = e.target.dataset.index;
                    VM.tabIndex = e.target.dataset.index;
                    if ($(e.target).hasClass('text')) {
                        VM.tabLeft = $(e.target).parent('.title').position().left + $(e.target).parent('.title').width() / 2 - VM.tabBarWidth / 2;
                    } else {
                        VM.tabLeft = $(e.target).position().left + $(e.target).width() / 2 - VM.tabBarWidth / 2;
                    }
                    if (VM.tabIndex == 0) {
                        VM.distributionName = '班级'
                        VM.distributionCharts('class')
                    } else {
                        VM.distributionCharts('resource')
                    }

                })
            } else {
                $('#loading-fail').removeClass('hidePreScore');
            }
        },
        error: function(err) {
            $('#loading-fail').removeClass('hidePreScore');
        }
    })

}

function loadRank() {
    var VM = new Vue({
        el: '#app',
        data: {
            account_id: account_id,
            totalScore: '',
            scoreResult: '',
            trophySrc: '',
            subTitle: '',
            class_id: class_id,
            rankData: '',
            rankLevel: 'class',
            myInfo: '',
            isShowLoading: true,
            province: '',
            isCheck: false,
            showClassInfo: true,
            noramalPage: true,
            opacity: 0,
            isShowScoreContainer: '',
            colorCompar: { bad: '#fc617b', good: "#fdb72b", excellent: "#11e392", noRank: "#1eb6e0" },
            errorCode: "",
            classData: "",
            resData: "",
            flag: false,
            tabBarLeft: '',
            tabWidth: "",
            tabBarWidth: '',
            tabIndex: "",
        },
        mounted: function() {
            this.tabIndex = window.isCheck
            if (class_id && class_id != -1) {
                this.loadData()
            } else if (class_id && class_id == -1) {
                this.isShowScoreContainer = false
                this.scoreResult = 'no-rank'
                this.trophySrc = 'images/d-trophy.png';
                this.subTitle = '你还没有进入预测分排名'
            }
            this.loadResData();

        },
        updated: function() {},
        computed: {
            tabBarStyle: function() {
                return {
                    '-webkit-transform': 'translateX(' + this.tabBarLeft + 'px)',
                    '-ms-transform': 'translateX(' + this.tabBarLeft + 'px)',
                    '-moz-transform': 'translateX(' + this.tabBarLeft + 'px)',
                    '-o-transform': 'translateX(' + this.tabBarLeft + 'px)',
                    transform: 'translateX(' + this.tabBarLeft + 'px)',
                }
            }
        },
        watch: {
            tabIndex: function(newVal, oldVal) {
                // 有all-rank这个容器的时候显示
                if (this.isShowScoreContainer) {
                    $('.all-rank-container').scrollTop(0)
                }

                newVal = Number(newVal)
                //下边栏右移 
                var selfLeft = $('.tab .item').eq(newVal).position().left;
                this.tabBarLeft = selfLeft + ($('.tab .item').eq(newVal).width() + parseFloat($('.tab .item').eq(newVal).css('padding-left'))*2 - $('.tabs-active-bar').width()) / 2;
                // tab改变数据改变,newVal==1,资源排名
                if (newVal) {
                    this.rankLevel = 'resource';
                    this.isShowScoreContainer = true;
                    // 高于或等于预测分总分的60%，将看到自己的区域排名

                    // 资源数据更新，资源请求成功才能tab,所以不存在请求失败的情况
                    this.dealData(this.resData, 'resource');

                } else {
                    if (class_id == -1) {
                        this.isShowScoreContainer = false
                        this.scoreResult = 'no-rank'
                        this.trophySrc = 'images/d-trophy.png';
                        this.subTitle = '你还没有进入预测分排名'
                    } else {
                        this.rankLevel = 'class'
                        if (this.classData) {
                            this.isShowScoreContainer = true;
                            this.dealData(this.classData, 'class')
                        } else {
                            this.isShowScoreContainer = false;
                            $('#class-loading-fail').removeClass('hidePreScore');
                        }

                    }

                }
            },
            scoreResult: function(newVal, oldVal) {},
            tabBarLeft: function(newVal, oldVal) {}

        },
        methods: {
            tabAddress: function(e) {
                this.tabIndex = e.target.dataset.index;

                // var selfLeft = $(e.target).position().left;
                // console.log(this.tabIndex)
                // this.tabBarLeft = selfLeft + ($(e.target).width() - this.tabBarWidth) / 2+Number($(e.target).css('padding-left').slice(0,-2));

            },
            // 请求接口数据保存以后对数据进行处理
            dealData: function(data, type) {
                if (!$('#class-loading-fail').hasClass('hidePreScore')) {
                    $('#class-loading-fail').addClass('hidePreScore')
                }
                var that = this;
                // 班级排名top10跟资源排名前top50,我的排名显示条件不同
                if (type == "class") {
                    var myInfo = data.filter(function(item) {
                        return item.account_id == that.account_id;

                    })
                    that.myInfo = myInfo[0];
                    if (data.length > 10) {
                        that.rankData = data.slice(0, 10);
                    } else {
                        that.rankData = data;
                    }
                } else if (type == 'resource') {
                    if (data.length > 50) {
                        that.rankData = data.slice(0, 50);
                    } else {
                        that.rankData = data
                    }
                    if (score / totalScore >= 0.6) {
                        var myInfo = {
                            rank: resRank,
                            name: name,
                            school: school,
                            predict_score: score,
                            portrait_cache: portrait_cache
                        }
                        this.myInfo = myInfo
                    } else {
                        this.myInfo = '';
                    }

                }
                if (that.rankData.length < 5) {
                    $(".less-five-person").removeClass('hidePreScore')
                    this.isShowScoreContainer = false
                } else {
                    $(".less-five-person").addClass('hidePreScore')
                }
                if (score) {
                    var scale = score / totalScore;
                    if (scale < 0.6) {
                        that.scoreResult = 'bad';
                        that.trophySrc = 'images/c-trophy.png';
                        that.subTitle = '你还有很大的提升空间哦!'
                    } else if (scale < 0.8) {
                        that.scoreResult = 'good'
                        that.trophySrc = 'images/b-trophy.png';
                        that.subTitle = '再加把劲，离优秀不远啦!'
                    } else {
                        that.scoreResult = 'excellent'
                        that.trophySrc = 'images/a-trophy.png';
                        that.subTitle = '你的成绩很棒哦，要保持!'
                    }
                } else {
                    that.scoreResult = 'no-rank'
                    that.trophySrc = 'images/d-trophy.png';
                    that.subTitle = '你还没有进入预测分排名'
                }
            },
            // 班级数据
            loadData: function(data, url) {
                var that = this

				if(god){
					var AU = 'http://teacher.ets100.com/point-service/class/rank';
				}else{
					var AU = API + '/point-service/class/rank';
				}
				
                $.ajax({
                    url: AU,
                    type: 'post',
                    dataType: 'json',
                    async: false,
                    data: {
                        token: token,
                        id: class_id,
                        size: 180
                    },
                    success: function(data) {
                        if (data.code == 0) {

                            that.classData = data.data
                        } else {
                            $('#class-loading-fail').removeClass('hidePreScore');
                            that.errorCode = data.code
                            that.scoreResult = 'no-rank'
                            that.trophySrc = 'images/d-trophy.png';
                            that.subTitle = '你还没有进入预测分排名'
                        }

                    },
                    error: function(err) {
                        // var that = this;
                        // $('#class-loading-fail').removeClass('hidePreScore');
                        // that.scoreResult = 'no-rank'
                        // that.trophySrc = 'images/d-trophy.png';
                        // that.subTitle = '你还没有进入预测分排名'
                        setBackButtonStyle(1)
                        $('#loading-fail').removeClass('hidePreScore');
                    }
                });
            },
            // 资源数据
            loadResData: function(data) {
                var that = this;

				if(god){
					var AU = 'http://teacher.ets100.com/point-service/resource/rank'
				}else{
					var AU = API + '/point-service/resource/rank'
				}

                $.ajax({
                    url: AU,
                    type: 'post',
                    dataType: 'json',
                    async: false,
                    data: {
                        token: token,
                        id: resource_id,
                        size: 180
                    },
                    success: function(data) {
                        if (data.code == 0) {

                            if (data.ext.city) {
                                that.province = data.ext.city
                                // that.province='北京市'
                            } else {
                                that.province = data.ext.province
                            }
                            that.resData = data.data


                            that.isShowLoading = false;
                            $('#app2').removeClass('hidePreScore');

                            $(".time-status-bar").height(statusHeight)
                            $(".head-operate-container").height(titleHeight);
                            // tab下方条滚动
                            var tabWidth = $('.tab .item').width();
                            var tabLeft = Number($('.tab .item').eq(0).css('padding-left').slice(0, -2));
                            var tabBarWidth = $('.tabs-active-bar').width();
                            var tabBarLeft = tabLeft + (tabWidth / 2) - (tabBarWidth / 2);
                            that.tabBarWidth = tabBarWidth;
                            that.tabWidth = tabWidth;
                            that.tabBarLeft = tabBarLeft;
                        } else {
                            setBackButtonStyle(1)
                            $('#loading-fail').removeClass('hidePreScore');

                        }
                    },
                    error: function() {
                        setBackButtonStyle(1)
                        $('#loading-fail').removeClass('hidePreScore');
                    }
                })
            },
            goSetting: function() {
                // VM.$destroy();
                setBackButtonStyle(1);
                if (/test/.test(API)) {
                    var apiUrl = "http://test.www.ets100.com"
                } else if (/hd/.test(API)) {
                    var apiUrl = "http://hdwww.ets100.com"
                } else {
                    var apiUrl = "http://www.ets100.com"
                }
                window.location.href = './setting.html?province=' + this.province + "&token=" + token + "&statusHeight=" + statusHeight + "&titleHeight=" + titleHeight + "&apiUrl=" + apiUrl;
            },
            addClass: function() {
                window.addClass()
            },
            goHelp: function() {
                setBackButtonStyle(1);
                window.location.href = './common-problem-detail.html?title=如何进入排名&statusHeight=' + statusHeight + "&titleHeight=" + titleHeight
            }
        },
        destroyed: function() {
            // alert('销毁实例')
        }
    })
    $('body').scroll(function(e) {
        var scrollTop = $('body').scrollTop();
        // console.log(scrollTop)
        var ifIos = window.isIos();
        if (ifIos) {
            // alert(scrollTop)
            $(".all-bar").css({ position: 'absolute', 'top': scrollTop });
        }
        var scoreContainer = $('.left-head').offset().top / 2;

        if (scrollTop >= scoreContainer) {
            VM.noramalPage = false;
            VM.opacity = 0;
            if (!VM.flag) {
                VM.flag = true;
            }
        } else if (scrollTop == 0) {
            VM.opacity = 0;
            VM.noramalPage = true;
            if (VM.flag) {
                VM.flag = false;
            }

        } else {
            // 变成初始状态
            if (VM.flag) {
                VM.flag = false;
            }
            // window.setBackButtonStyle(0)
            VM.noramalPage = true;
            VM.opacity = 0.1;

        }
    })
}

function computeOpenType(open_type) {
    // var open_type = open_type || 31;
    // var res = {};
    // res.listenOpen = (open_type & 1) == 1 ? 1 : 0;
    // res.speakOpen = (open_type & 2) == 2 ? 1 : 0;
    // res.readOpen = (open_type & 4) == 4 ? 1 : 0;
    // res.writeOpen = (open_type & 8) == 8 ? 1 : 0;
    // res.viewOpen = (open_type & 16) == 16 ? 1 : 0;
    // return res
    var open_type = open_type === undefined ? 0 : open_type;
    var res = {};
    res.listenOpen = (open_type & 1) == 1 ? 1 : 0;
    res.speakOpen = (open_type & 2) == 2 ? 1 : 0;
    res.readOpen = (open_type & 4) == 4 ? 1 : 0;
    res.writeOpen = (open_type & 8) == 8 ? 1 : 0;
    res.viewOpen = (open_type & 16) == 16 ? 1 : 0;
    return res
}


function setConcatName(options) {
    var tags = options.tags,
        concat_name = options.concat_name;
    for (var i in tags) {
        var tag = tags[i];
        var name = tag.name;
        tag.concat_name = concat_name + ' | ' + name
        if (tag.child) {
            setConcatName({
                tags: tag.child,
                concat_name: tag.concat_name
            })
        }
    }
}
// 学情图谱部分
function loadLearning() {
    $.ajax({
        url: API + '/point-service/account/info',
        type: 'POST',
        dataType: 'json',
        data: {
            id: account_id,
            resource_id: resource_id,
            token: token
        },
        success: function(data) {
            if (data.code == 0) {
                var vacants = [true, true, true, true, true];
                var scoreAverage = [];
                var score = ["noScore", "noScore", "noScore", "noScore", "noScore"];
                var tabIndex = 0;
                var res = {}
                // label为空，没有开通这个模块 
                if (data.data) {
                    res = computeOpenType(data.ext.open_type);
                    if (res.listenOpen == 0) {
                        vacants[0] = false
                    } else {
                        if (data.data.listen_label !== '{}') {
                            score[0] = ''
                        }
                    }
                    if (res.speakOpen == 0) {
                        vacants[1] = false
                    } else {
                        if (data.data.speak_label !== '{}') {
                            score[1] = ''
                        }
                    }
                    if (res.readOpen == 0) {
                        vacants[2] = false
                    } else {
                        if (data.data.read_label !== '{}') {
                            score[2] = ''
                        }
                    }
                    if (res.writeOpen == 0) {
                        vacants[3] = false
                    } else {
                        if (data.data.write_label !== '{}') {
                            score[3] = ''
                        }
                    }
                    if (res.viewOpen == 0) {
                        vacants[4] = false
                    } else {
                        if (data.data.view_label !== '{}') {
                            score[4] = ''
                        }
                    }
                }
                for (var i = 0; i < vacants.length; i++) {
                    if (vacants[i]) {
                        tabIndex = i;
                        break
                    }
                }
                setConcatName({
                    tags: data.tags,
                    concat_name: ''
                })
                // 把数据从账号成长接口上抽离出来
                var listenRate = [];
                var speakRate = [];
                var readRate = [];
                var writeRate = [];
                var viewRate = [];
                var pdates = [];
                var tabLabelTextObj = {};
                $.ajax({
                    url: API + '/point-service/account/trail',
                    type: 'post',
                    dataType: 'json',
                    data: {
                        id: account_id,
                        resource_id: resource_id,
                        token: token,
                        size: 180
                    },
                    success: function(chartData) {
                        var VM = new Vue({
                            el: '#app',
                            data: {
                                tabBarLeft: '',
                                tabWidth: "",
                                tabBarWidth: '',
                                tabIndex: tabIndex,
                                // 该模块有没有分数
                                score: score,
                                // 该能力有没有开通
                                vacants: vacants,
                                titleArr: ["听", "说", "读", "写", "看"],
                                data: data,
                                isShowLoading: true,
                                loadingFlag: "",
                                toptag: {},
                                tagindex: 1,
                                growingLineHidden: false,
                                tagsArrForname: [],
                                nowParentTagId: [],
                                childItems: [],
                                tagDemoText: '',
                                tabLabelTextObj: tabLabelTextObj
                            },
                            mounted: function() {
                                $('.container').removeClass('hidePreScore');
                                var isIos = window.isIos();
                                var iosDp = $('html').attr('data-dpr');
                                if (isIos) {
                                    $('.head-operate-container').height(88);
                                    $('.out-on-off').css('fontSize', 18 * iosDp + 'px');
                                } else {
                                    $('.out-on-off').css('fontSize', '16px');
                                }
                                $(".time-status-bar").height(statusHeight)
                                $(".head-operate-container").height(titleHeight).css("line-height", titleHeight + 'px');
                                $('.scroll-container,.child-tag').css('padding-top', $('.all-bar').height())
                                var tabWidth = $('.tab .item').width();
                                var tabLeft = $('.tab .item').eq(0).position().left;
                                var tabBarWidth = $('.tabs-active-bar').width()
                                var tabBarLeft = tabLeft + (tabWidth / 2) - (tabBarWidth / 2);
                                this.tabBarWidth = tabBarWidth;
                                this.tabWidth = tabWidth;
                                this.tabBarLeft = tabBarLeft;
                            },
                            updated: function() {
                                var that = this;
                                if(this.childItems.length==0){
                                    var isIos = window.isIos();
                                    var iosDp = $('html').attr('data-dpr');
                                    if (isIos) {
                                        $('.ability-items .item .text').css('fontSize', '0.32rem');
                                        $('.ability-items .item .text .unit').css('fontSize', '0.267rem');
                                    }
                                    if (this.vacants[this.tabIndex]) {
                                        createCircle(this, this.data.tags);
                                    } else {
                                        this.loadingFlag = true
                                    }
                                    // 更新nowParentTagId绘制canvas
                                    var item = this.data.tags[this.nowParentTagId[0]];
                                    if (this.nowParentTagId[0]) {
                                        if (!$('.big-canvas.canvas' + item.id).parents('.child-tag').hasClass('drawed')) {
                                            setTimeout(function() {
                                                that.setCSS();
                                                createCircle(that, item.child);
                                                createBigCircle(that, item);
                                                $('.big-canvas.canvas' + item.id).parents('.child-tag').addClass('active drawed');
                                            }, 0)
                                        }
                                    }
                                }else{
                                    var item = this.childItems[that.childItems.length-1];
                                    setTimeout(function(){
                                        that.setCSS();
                                        var childTag = $("#"+item.id);
                                        $('.child-tag.active .child-tag-scroll').scrollTop(0);
                                        childTag.children('.child-tag').addClass('active drawed');
                                        createCircle(that, item.child);
                                        createBigCircle(that, item);
                                        this.growingLineHidden = true;
                                    },0)
                                }
                            },
                            methods: {
                                setCSS: function(){
                                    var isIos = window.isIos();
                                    var iosDp = $('html').attr('data-dpr');
                                    if (isIos) {
                                        $('.head-operate-container').height(88);
                                        $('.out-on-off').css('fontSize', 18 * iosDp + 'px');
                                        $('.ability-items .item .text').css('fontSize', '0.32rem');
                                        $('.ability-items .item .text .unit').css('fontSize', '0.267rem');
                                    } else {
                                        $('.out-on-off').css('fontSize', '16px');
                                    }
                                    $(".time-status-bar").height(statusHeight);
                                    $(".head-operate-container").height(titleHeight).css("line-height", titleHeight + 'px');
                                    var h = $('.all-bar').height() + 'px'
                                    $('.child-tag-scroll').css("padding-top", h)
                                    // $('.child-tag-scroll .scroll-con').css('height', 'calc(100% - ' + h + ')');
                                    // $('.child-tag-scroll .scroll-con').css('height', '100%');
                                },
                                tabAddress: function(e) {
                                    if (this.tabIndex != e.target.dataset.index) {
                                        this.loadingFlag = false;
                                        this.tabIndex = e.target.dataset.index;
                                        var selfLeft = $(e.target).position().left;
                                        this.tabBarLeft = selfLeft + (this.tabWidth - this.tabBarWidth) / 2;
                                        $('.growing-container1').width($('.tab').width());
                                        growChart.dispatchAction({
                                            type: 'hideTip'
                                        })
                                        setTimeout(function() {
                                            if (e.target.dataset.index == 1) {
                                                makeCharts($('.growing-container1')[0], pdates, speakRate)
                                            } else if (e.target.dataset.index == 2) {
                                                makeCharts($('.growing-container1')[0], pdates, readRate)
                                            } else if (e.target.dataset.index == 3) {
                                                makeCharts($('.growing-container1')[0], pdates, writeRate)
                                            } else if (e.target.dataset.index == 4) {
                                                makeCharts($('.growing-container1')[0], pdates, viewRate)
                                            } else if (e.target.dataset.index == 0) {
                                                makeCharts($('.growing-container1')[0], pdates, listenRate)
                                            }
                                        }, 0)
                                    }
                                },
                                goHelp: function() {
                                    setBackButtonStyle(1);
                                    window.location.href = './common-problem-detail.html?title=能力标签得分率&statusHeight=' + statusHeight + "&titleHeight=" + titleHeight
                                },
                                getItemScore: function(item) {
                                    var that = this;
                                    var nowOpenText = openTypeTextArr[that.tabIndex];
                                    var openStatu = computeOpenType(item.open_type);
                                    var score = 0;
                                    if (openStatu[nowOpenText]) {
                                        var label = JSON.parse(that.data.data[nowOpenText.replace('Open', '_label')].replace(/\'/g, "\""));
                                        if (label[item.id]) {
                                            score = Math.round(label[item.id] * 100)
                                        }
                                    }
                                    return score
                                },
                                isOpenTag: function(item) {
                                    var that = this;

                                    var nowOpenText = openTypeTextArr[that.tabIndex];
                                    var openStatu = computeOpenType(item.open_type);
                                    if (openStatu[nowOpenText]) {
                                        return true
                                    }
                                    return false
                                },
                                getTagLevelTitle: function(tag) {
                                    var that = this;
                                    var str = this.titleArr[this.tabIndex]; // 听说读写看
                                    var l = this.tagsArrForname.length - 1;
                                    for (var i = 0; i < this.tagsArrForname.length; i++) {
                                        str += ' | ' + this.tagsArrForname[i].name // 加上其余的name
                                    }
                                    return {
                                        allText: str,
                                        lastText: l > -1 ? that.tagsArrForname[l]['name'] : ''
                                    }
                                },
                                subStrName: function(string) {
                                    if (string.length > 5) {
                                        return string.substr(0, 5) + '\n' + string.substr(5)
                                    } else {
                                        return string
                                    }
                                },
                                goNextTag: function(e, item) {
                                    var dom = $(e.currentTarget);
                                    var tags = this.data.tags;
                                    var nowParentTagId = this.nowParentTagId;
                                    var childTag = dom.siblings(".child-tag-contain").children('.child-tag');
                                    if (item.id != nowParentTagId[0] && tags[item.id]) {
                                        this.nowParentTagId[0] = item.id;
                                    }
                                    this.tagsArrForname.push(item);
                                    var l = $('.child-tag.active').length;
                                    if (!childTag.hasClass('drawed') && item.id != nowParentTagId[0] && nowParentTagId[0]) {
                                        this.nowParentTagId.push(item.id);
                                        this.childItems.push(item);
                                    }else{
                                        this.growingLineHidden = true;
                                        $('.child-tag.active .child-tag-scroll').scrollTop(0);
                                        childTag.addClass('active drawed');
                                    }
                                    
                                },
                                showChildDom: function(tag){
                                    return this.childItems.filter(function(item){
                                        return item.id == tag.id
                                    }).length > 0
                                },
                                showGrowChartTooltip: function() {
                                    growChart.dispatchAction({
                                        type: 'showTip',
                                        seriesIndex: 0, //第几条series
                                        dataIndex: pda.length - 1, //第几个tooltip
                                    });
                                },
                                showDemo: function(tag) {
                                    this.tagDemoText = tag.demo
                                },
                                hideDemo: function() {
                                    this.tagDemoText = ''
                                }
                            },
                            watch: {
                                loadingFlag: function(newVal, oldVal) {
                                    if (newVal) {
                                        this.isShowLoading = false;
                                    } else {
                                        this.isShowLoading = true;
                                    }
                                }
                            },
                            computed: {
                                tabLabelText: function() {
                                    var index = this.tabIndex;
                                    return this.tabLabelTextObj[tabLabelKeyArr[index]]
                                },
                                tabBarStyle: function() {
                                    return {
                                        '-webkit-transform': 'translateX(' + this.tabBarLeft + 'px)',
                                        '-ms-transform': 'translateX(' + this.tabBarLeft + 'px)',
                                        '-moz-transform': 'translateX(' + this.tabBarLeft + 'px)',
                                        '-o-transform': 'translateX(' + this.tabBarLeft + 'px)',
                                        transform: 'translateX(' + this.tabBarLeft + 'px)',
                                    }
                                },
                                title: function() {
                                    return this.titleArr[this.tabIndex]
                                }
                            },
                            filters: {
                                subStr: function(string) {
                                    if (string.length > 5) {
                                        return string.substr(0, 5) + '\n' + string.substr(5)
                                    } else {
                                        return string
                                    }
                                }
                            },
                        })
                        window.VM = VM;
                        // 当前学生的听,说,读，写，看能力还有日期数据
                        if (chartData.data && chartData.data.length !== 0) {
                            chartData.data.forEach(function(item, index) {
                                pdates.unshift(item.pdate.substring(5));
                                listenRate.unshift(item.listen_rate);
                                speakRate.unshift(item.speak_rate);
                                readRate.unshift(item.read_rate);
                                writeRate.unshift(item.write_rate);
                                viewRate.unshift(item.view_rate);
                            })
                            // if (pdates.length % 2 == 1) {
                            //     pdates = [{ value: pdates[0], textStyle: { padding: [0, 0, 0, 30 * dps] } }, pdates[(pdates.length - 1) / 2], { value: pdates[pdates.length - 1], textStyle: { padding: [0, 40 * dps, 0, 0] } }]
                            // } else {
                            //     pdates = [{ value: pdates[0], textStyle: { padding: [0, 0, 0, 20 * dps] } }, pdates[(pdates.length - 2) / 2], { value: pdates[pdates.length - 1], textStyle: { padding: [0, 40 * dps, 0, 0] } }]
                            // }
                            var firstDate = pdates[0];
                            var lastDate = pdates[pdates.length - 1]
                            pdates.forEach(function(item, index) {
                                if (index == 0) {
                                    pdates[0] = {}
                                    pdates[0].value = firstDate;
                                    pdates[0].textStyle = {}
                                    pdates[0].textStyle.padding = [0, 0, 0, 30 * dps]
                                } else if (index == pdates.length - 1) {
                                    pdates[index] = {}
                                    pdates[index].value = lastDate;
                                    pdates[index].textStyle = {}
                                    pdates[index].textStyle.padding = [0, 40 * dps, 0, 0]
                                }
                            })
                            $('.growing-container1').width($('.tab').width());
                            if (res.listenOpen) {
                                makeCharts($('.growing-container1')[0], pdates, listenRate)
                            } else if (res.speakOpen) {
                                makeCharts($('.growing-container1')[0], pdates, speakRate)
                            } else if (res.readOpen) {
                                makeCharts($('.growing-container1')[0], pdates, readRate)
                            } else if (res.writeOpen) {
                                makeCharts($('.growing-container1')[0], pdates, writeRate)
                            } else if (res.viewOpen) {
                                makeCharts($('.growing-container1')[0], pdates, viewRate)
                            }
                        }
                        if (chartData.ext) {
                            VM.tabLabelTextObj = chartData.ext.label_text ? chartData.ext.label_text : {}
                        }
                    }
                })
            } else {
                setBackButtonStyle(1)
                $('#loading-fail').removeClass('hidePreScore');
            }
        },
        error: function(err) {
            setBackButtonStyle(1)
            $('#loading-fail').removeClass('hidePreScore');
        }
    })
}

//god rank 
// var statusHeight=20;
// var titleHeight=44;
// API = 'https://hdapi.ets100.com'
// preScorePara('{"ecard_id":"4613089","class_id":"31450","res_id":"67","token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6Ijk1NzQ4OCIsImxvZ2luIjoiMTU2MjUxMDA4ODkiLCJjcmVhdGUiOjE1NTAyODgyNjR9.C3L4N7Nm5yWSeszRZwVHXGlZaxtEb8gdbICKYN-phao","isCheck":0,"score":"102.0","totalScore":"150.0","name":"许岳坤","portrait_cache":"http://cdn.fei.ets100.com/portrait/0600c4a2ad4dd77bc12706c72bc1a12d.jpg","school":"广州五中","resRank":"11177"}','rank')