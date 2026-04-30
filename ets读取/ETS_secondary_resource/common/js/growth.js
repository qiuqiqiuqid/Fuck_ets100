/*
 * @Author: jshuang2
 * @Date: 2022-06-06 17:56:50
 * @LastEditors: jshuang2
 * @LastEditTime: 2022-07-05 10:07:50
 * @Description: 请填写简介
 */
var dps = document.documentElement.clientWidth / 375;
getHeight();
var hideJoinClass = GetQueryString('hideJoinClass');
if (hideJoinClass) {
    $('.add-class').hide();
}
$('#growthCon').css('padding-top', $('.all-bar').height())
var growChart = null;
// 学情图谱
// 图表
function makeCharts(dom, dataX, dataY) {
    growChart = echarts.init(dom);
    // 班级预测分图表
    var option = {
        grid: {
            left: 0,
            right: 5 * dps,
            top: 10 * dps,
            bottom: 0,
            containLabel: true
        },
        tooltip: {
            trigger: 'axis',
            formatter: function (params) {
                return "日期：" + params[0].axisValue + "<br/> 得分率：" + Math.round(params[0].data * 100) + "%"
            },
            backgroundColor: '#fff',
            // borderColor: 'rgba(0,0,0,0.2)',
            // borderWidth: 1,
            textStyle: {
                color: '#979797',
                fontSize: 14 * dps
            },
            axisPointer: {
                type: 'none'
            }
        },
        xAxis: [{
            type: 'category',
            boundaryGap: false,
            data: dataX,
            axisTick: {
                show: false
            },
            axisLabel: {
                color: '#979797',
                fontSize: 11 * dps,
                interval: function (index, value) {
                    if (index == 0 || index == parseInt(dataX.length / 2) || index == dataX.length - 1) {
                        return true
                    }
                }
            },
            axisLine: {
                lineStyle: {
                    color: '#E6E6E6'
                },
                show: false
            },
        }],
        yAxis: [{
            type: 'value',
            boundaryGap: true,
            axisTick: {
                inside: true,
                show: false
            },
            axisLine: {
                show: false,
            },
            axisLabel: {
                color: '#979797',
                fontSize: 11 * dps,
                formatter: function (val, index) {
                    return Math.round((val * 100)) + "%"
                }
            },
            splitLine: {
                lineStyle: {
                    color: '#e6e6e6',
                    width: 0.5 * dps
                }
            },
            max: 1
        }],
        series: [{
            name: "成绩",
            type: 'line',
            smooth: true,
            areaStyle: {
                color: {
                    type: 'linear',
                    x: 0,
                    y: 0,
                    x2: 0,
                    y2: 1,
                    colorStops: [{
                        offset: 0,
                        color: 'rgba(99,217,139,1)' // 0% 处的颜色
                    }, {
                        offset: 1,
                        color: 'rgba(99,217,139,0.1)' // 100% 处的颜色
                    }],
                    globalCoord: false // 缺省为 false
                }
            },
            itemStyle: {
                color: '#63D98B '
            },
            // y轴的能力分数
            // data: [45, 89, 36]
            data: dataY
        }]
    };
    setTimeout(function () {
        growChart.dispatchAction({
            type: 'showTip',
            seriesIndex: 0,//第几条series
            dataIndex: dataX.length - 1,//第几个tooltip
        });
    }, 0)
    if (option && typeof option === "object") {
        growChart.setOption(option, true);
        growChart.resize();
    }
}
Vue.component('child-tag', {
    props: ['tag', 'gettitle', 'isopentag', 'canvas', 'getitemscore', 'substrname', 'gonexttag', 'titlearr', 'tabindex', 'showdemo', 'showchilddom'],
    template: '<div class="child-tag">' +
        '<div class="child-tag-scroll">' +
        '<div class="all-bar scrolling" style="display:block;">' +
        '<div class="time-status-bar"></div>' +
        '<div class="tag-top-name head-operate-container">{{tag.name}}</div>' +
        '</div>' +
        '<div class="scroll-con">' +
        '<div class="tag-name-l">{{titlearr[tabindex]+tag.concat_name}}</div>' +
        '<div class="big-canvas-contain">' +
        '<canvas class="big-canvas" :class="canvas+tag.id"></canvas>' +
        '<div class="text">' +
        '<div><span class="num">{{getitemscore(tag)}}</span><span class="unit">%</span></div>' +
        '<div>得分率</div>' +
        '</div>' +
        '</div>' +
        '<div class="tag-desc">{{tag.desc}}<span class="tag-example" v-if="tag.demo" @click="showdemo(tag)">示例</span></div>' +
        '<div class="ability-items">' +
        '<div class="item-container" v-if="isopentag(item)" v-for="(item, index) in tag.child">' +
        '<div class="item" @click.stop.prevent="gonexttag($event,item,false)">' +
        '<canvas class="small-canvas" :class="canvas+index" width="220" height="220"></canvas>' +
        '<div class="text">{{getitemscore(item)}}<span class="unit">%</span></div>' +
        '</div>' +
        '<div class="note" @click.stop.prevent="gonexttag($event,item,false)">{{substrname(item.name)}}<span class="note-arrow"></span></div>' +
        '<div class="child-tag-contain" v-if="showchilddom(item)" :id="item.id">' +
        '<child-tag :tag="item" :gettitle="gettitle" :isopentag="isopentag" canvas="canvas" :getitemscore="getitemscore" :substrname="substrname" :gonexttag="gonexttag" :titlearr="titlearr" :tabindex="tabindex" :showdemo="showdemo" :showchilddom="showchilddom"></child-tag>' +
        '</div>' +
        '</div>' +
        '</div>' +
        '</div>' +
        '</div>' +
        '</div>'
})

// 能力标签canvas
function createCircle(that, tags) {
    var index = that.tabIndex;
    // var tags = that.data.tags;
    // // 绘制能力标签得分率圆环
    var nowOpenText = openTypeTextArr[that.tabIndex];
    var color = '';
    for (var i in tags) {
        var tag = tags[i];
        var openStatu = computeOpenType(tag.open_type);
        if (openStatu[nowOpenText] && that.data.data) {
            var label = JSON.parse(that.data.data[nowOpenText.replace('Open', '_label')].replace(/\'/g, "\""));
            var score = label[i] ? label[i] : 0;
            if (score <= 0.6) {
                color = '#ff6268'
            } else if (score <= 0.8) {
                color = '#ffa600'
            } else {
                color = '#3ad56f'
            }
            var canvas = $('#app2').find(".small-canvas.canvas" + i)[0];
            canvas.setAttribute('width', 220);
            canvas.setAttribute('height', 220);
            var context = canvas.getContext('2d');
            canvas.width = canvas.width * dps;
            canvas.height = canvas.height * dps;
            var x = canvas.width / 8,
                y = canvas.height / 8;
            context.save();
            context.scale(4, 4);
            context.lineCap = 'round';
            context.lineWidth = 2 * dps;

            context.beginPath();
            context.arc(x, y, 25.5 * dps, 0, 2 * Math.PI, false);
            context.strokeStyle = '#e6e6e6';
            context.stroke();

            context.strokeStyle = color;

            // 画色
            context.beginPath();
            context.arc(x, y, 25.5 * dps, -Math.PI / 2, -Math.PI / 2 + (score) * 2 * Math.PI, false);
            context.stroke();
            context.closePath();
            context.restore();
            context = null;
        }
    }
    that.loadingFlag = true;
}

function createBigCircle(that, tag) {
    var index = that.tabIndex;
    // var tags = that.data.tags;
    // // 绘制能力标签得分率圆环
    var nowOpenText = openTypeTextArr[that.tabIndex];
    var color = '';
    var openStatu = computeOpenType(tag.open_type);
    var i = tag.id;
    if (openStatu[nowOpenText] && that.data.data) {
        var label = JSON.parse(that.data.data[nowOpenText.replace('Open', '_label')].replace(/\'/g, "\""));
        var score = label[i] ? label[i] : 0;
        if (score <= 0.6) {
            color = '#ff6268'
        } else if (score <= 0.8) {
            color = '#ffa600'
        } else {
            color = '#3ad56f'
        }
        var canvas = $('#app2 .content').find(".big-canvas.canvas" + i)[0];
        var width = $("#app2 .big-canvas").width();
        var height = $("#app2 .big-canvas").height();
        var context = canvas.getContext('2d');
        canvas.width = width < document.documentElement.clientWidth ? width : 150;
        canvas.height = height < document.documentElement.clientWidth ? height : 150;
        var lineWidth = 8 * (dps > 1 ? dps : 1);
        var x = canvas.width / 2,
            y = canvas.height / 2,
            r = (width - lineWidth) / 2;
        context.save();
        context.lineWidth = lineWidth;
        context.lineCap = 'round';

        context.beginPath();
        context.arc(x, y, r, 0, 2 * Math.PI, false);
        context.strokeStyle = '#e6e6e6';
        context.stroke();

        context.strokeStyle = color;

        // 画色
        context.beginPath();
        context.arc(x, y, r, -Math.PI / 2, -Math.PI / 2 + (score) * 2 * Math.PI, false);
        context.stroke();
        context.closePath();
        context.restore();
        context = null;
    }
    that.loadingFlag = true;
}



// 预测分
// 个人成长轨迹图 班级排名轨迹 省级排名轨迹 函数
function drawGrowChart(dom, dates, scores, yName, max) {
    if (yName == '成绩') {
        var inverse = false;
        var origin = 'start';
        var min = null;
        var max = max
    } else if (yName == '排名') {
        var inverse = true;
        var origin = 'end';
        var min = "dataMin";
        var max = null

    }
    // $('#growing-container1').width('1000px')
    var growChart = echarts.init(dom);
    var option = {
        title: {
            text: '',
            textStyle: {
                fontSize: 14 * dps,
                fontWeight: 'bold'
            }
        },
        grid: {
            left: 0,
            right: 8 * dps,
            top: 10 * dps,
            bottom: 0,
            containLabel: true
        },
        tooltip: {
            trigger: 'axis',
            formatter: function (params) {
                return "日期：" + params[0].axisValue + " <br/>" + yName + "：" + params[0].data
            },
            backgroundColor: '#fff',
            textStyle: {
                color: '#979797',
                fontSize: 12 * dps,
                padding: 10
            },
            axisPointer: {
                type: 'none'
            }
        },
        xAxis: [{
            type: 'category',
            boundaryGap: false,
            data: dates,
            axisTick: {
                show: false,
                align: 'right',
                alignWithLabel: true
            },
            axisLabel: {
                color: '#979797',
                fontSize: 11 * dps,
                padding: [0, 40 * dps, 0, 30 * dps],
                interval: function (index, value) {
                    if (index == 0 || index == parseInt(dates.length / 2) || index == dates.length - 1) {
                        return true
                    }
                }
            },
            axisLine: {
                lineStyle: {
                    color: '#E6E6E6',

                },
                show: false
            }
        }],
        yAxis: [{
            type: 'value',
            boundaryGap: true,
            axisTick: {
                inside: true,
                show: false
            },
            axisLine: {
                show: false,
                fontSize: 11 * dps,
                // color:'#eee'

            },
            axisLabel: {
                color: '#979797',
                fontSize: 11 * dps,
                formatter: function (val, index) {
                    // if(Number.isInteger(val)){
                    //     return val
                    // }
                    return val % 1 == 0 ? val : ""
                    // if(val){
                    //     return val
                    // }
                }
            },
            splitLine: {
                lineStyle: {
                    color: '#e6e6e6',
                    width: 0.5 * dps
                },
                show: true
            },
            inverse: inverse,
            min: min,
            max: max
        }],
        series: [{
            name: "成绩",
            type: 'line',
            smooth: true,
            areaStyle: {
                origin: origin,
                color: {
                    type: 'linear',
                    x: 0,
                    y: 0,
                    x2: 0,
                    y2: 1,
                    colorStops: [{
                        offset: 0,
                        color: 'rgba(99,217,139,1)' // 0% 处的颜色
                    }, {
                        offset: 1,
                        color: 'rgba(99,217,139,0.1)' // 100% 处的颜色
                    }],
                    globalCoord: false // 缺省为 false
                }
            },
            itemStyle: {
                color: '#63D98B '
            },
            data: scores
        }]
    };
    if (option && typeof option === "object") {
        growChart.setOption(option, true);

    }
}

// 班级分布图 省级分布图
function distributionChart(dom, data, coord, maxX) {
    var offsetScale;
    if (!coord[1]) {
        offsetScale = 0;
        var markPoint = {}
    } else {
        var offsetScale = 1 - (coord[1] / VM1.scores.total_point);
        var markPoint = {
            symbol: 'circle',
            symbolSize: 10 * dps,
            label: {
                show: true,
                position: [10, -16],
                formatter: [
                    '{a|你在这里}'

                ].join('\n'),
                rich: {
                    a: {
                        color: '#979797',
                        fontSize: 12 * dps,
                        backgroundColor: {
                            image: './images/here-bubble.png',
                        },
                        width: 50 * dps,
                        height: 21 * dps,
                        padding: 10 * dps
                    }
                }

            },
            data: [{
                name: '某个坐标',
                // coord: [24, 60]
                coord: coord
            }]
        }
    }
    var growChart = echarts.init(dom);
    // var maxX = 5;
    var option = {
        title: {
            text: '',
            textStyle: {
                fontSize: 14 * dps,
                fontWeight: 'bold'
            }
        },
        grid: {
            left: 0,
            right: 30 * dps,
            bottom: 0,
            top: 10 * dps,
            containLabel: true
        },
        xAxis: [{
            type: 'value',
            name: '人数',
            nameTextStyle: {
                color: '#979797'
            },
            axisTick: {
                show: false
            },
            axisLabel: {
                color: '#979797',
                formatter: function () {
                    return "";
                },
                fontSize: 11 * dps
            },
            axisLine: {
                lineStyle: {
                    color: '#E6E6E6'
                },
                show: true,
                symbol: ['none', 'arrow'],
                symbolSize: [5, 10]
            },
            splitLine: {
                show: false
            },
            max: maxX * 1.5,
            nameGap: 5
        }],
        yAxis: [{
            type: 'value',
            nameTextStyle: {
                color: '#979797'
            },
            axisTick: {
                show: false
            },
            axisLine: {
                lineStyle: {
                    color: '#e6e6e6'
                },
                show: true,
                symbolSize: [10, 20]
            },
            axisLabel: {
                color: '#979797',
                formatter: '{value}分',
                fontSize: 11 * dps
            },
            splitLine: {
                show: false,
                color: '#e6e6e6'
            }
        }],
        series: [{
            name: '人数分布',
            type: 'line',
            areaStyle: {
                color: {
                    type: 'linear',
                    x: 0,
                    y: 0,
                    x2: 0,
                    y2: 1,
                    colorStops: [{
                        offset: 0,
                        color: 'rgba(99,217,139,0.2)' // 0% 处的颜色
                    }, {
                        offset: offsetScale,
                        color: 'rgba(99,217,139,0.2)' // 0% 处的颜色
                    }, {
                        offset: offsetScale,
                        color: 'rgba(99,217,139,0.4)' // 0% 处的颜色
                    }, {
                        //     offset: 0.67,
                        //     color: 'rgba(99,217,139,0.4)' // 100% 处的颜色
                        // },
                        // {
                        //     offset: 0.67,
                        //     color: 'transparent' // 100% 处的颜色
                        // }, {
                        offset: 1,
                        // color: 'transparent'
                        color: 'rgba(99,217,139,0.4)'
                    }],
                    globalCoord: false // 缺省为 false
                }
            },
            itemStyle: {
                color: '#63D98B'
            },
            showSymbol: false,
            smooth: true,
            // data: [
            //     [23, 50],
            //     [24, 60],
            //     [14, 70],
            //     [35, 80],
            //     [15, 90],
            //     [0, 150]
            // ],
            data: data,
            markPoint: markPoint
        }]
    }
    if (option && typeof option === "object") {
        growChart.setOption(option, true);

    }
}
// 数据排序生成
function compare(pro) {
    return function (obj1, obj2) {
        var val1 = obj1[pro];
        var val2 = obj2[pro];
        if (val1 < val2) { //正序
            return 1;
        } else if (val1 > val2) {
            return -1;
        } else {
            return 0;
        }
    }
}
// 页面重新加载
$(document).on('click', '#loading-fail-1, #loading-fail-2', function () {
    window.location.reload()
})
// 点击问号，跳转到常见问题列表
$(document).on('click', '#helpIcon', function (e) {
    e.stopPropagation();
    setBackButtonStyle(1);
    window.location.href = './common-problem-list.html?statusHeight=' + statusHeight + "&titleHeight=" + titleHeight
})
// 点击切换tab
$(document).on('click', '.growth-tab .item', function (e) {
    e.stopPropagation();
    sessionStorage.setItem('growthTabIndex', $(this).index());
    var index = $(this).index() + 1;
    var bar = $(this).siblings('.tabs-active-bar');
    $("#app" + index).show().siblings().hide();
    var left = $(this).width() * (index - 0.5) - bar.width() / 2;
    $(this).addClass('active').siblings('.item').removeClass('active')
    bar.css({
        '-webkit-transform': 'translateX(' + left + 'px)',
        '-ms-transform': 'translateX(' + left + 'px)',
        '-moz-transform': 'translateX(' + left + 'px)',
        '-o-transform': 'translateX(' + left + 'px)',
        'transform': 'translateX(' + left + 'px)',
    })
})
// 从客户端取出参数 
function preScoreParaGrowth(para) {
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
    loadPreScore()
}

function loadPreScore() {

    var dps = document.documentElement.clientWidth / 375;
    // dp = 1;
    var VM1 = new Vue({
        el: '#app1',
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
            latePdate: {},
            scores: '',
            tabRes: '',
            tabResAdd: "",
            // 是否显示资源排名轨迹
            isShowResRank: "",
            tabBarWidth: ""

        },
        methods: {
            tabAddress: function (e) { },
            goHelp: function () {
                setBackButtonStyle(1);
                window.location.href = './common-problem-list.html?statusHeight=' + statusHeight + "&titleHeight=" + titleHeight
            },
            // 分布图
            distributionCharts: function (tabName) {
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
                    success: function (chartData) {
                        if (chartData.data) {
                            var maxData = 0;
                            chartData.data.forEach(function (item, index) {
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
                            chartData.data.forEach(function (item, index) {
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
                                selfLoc = distributionDatas.filter(function (item, index) {
                                    return item[1] == VM1.scores.total_score;

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
            selfGrow: function () {
                var that = this;
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
                    success: function (chartData) {
                     //  console.log('chartData------------0请求trail成功')
                        loadLearning(chartData) // 加载学情图谱数据
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
                        chartData.data.forEach(function (item, index) {
                            if (index === 0) {
                                that.latePdate = item.pdate
                                that.scores = {
                                    total_score: item.predict_score,
                                    total_point: chartData.ext ? chartData.ext.total_point / 100 : 100
                                }
                            }
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
                        var latestData = chartData.data.filter(function (item) {
                            return item.pdate == latestDate
                        })
                        if (latestData[0] && latestData[0].class_rank > 0) {
                            VM1.selfRank = latestData[0].class_rank;
                        } else {
                            VM1.selfRank = '--'
                        }
                        if(chartData.data.length > 0) {
                            setTimeout(function () {
                                // 预测分图表
                                var dom = document.getElementById('growing-container');
                                // 班级排名container
                                var dom1 = document.getElementById('growing-container1');
                                var firstDate = pdates[0];
                                var lastDate = pdates[pdates.length - 1]
                                pdates.forEach(function (item, index) {
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
                                drawGrowChart(dom, pdates, predict_scores, '成绩', VM1.scores.total_point)
                                // 省级排名container
                                if (class_id != -1) {
                                    drawGrowChart(dom1, pdates, class_ranks, '排名');
                                }
                               
                            }, 0)
                        }
                        that.drawScores(that.scores || 0)
                        // VM1.selfRank = latestData[0].class_rank;
                         // 班级，省级排名tab
                        $('.tab .title').on('click', function (e) {
                            if (e.target.dataset.index == 0) {
                                // 班级排名
                                VM1.distributionName = '班级';
                                if (latestData[0].class_rank > 0) {
                                    VM1.selfRank = latestData[0].class_rank;
                                } else {
                                    VM1.selfRank = '--'
                                }
                                // VM1.selfRank = latestData[0].class_rank;
                            } else if (e.target.dataset.index == 1) {
                                // 省市排名
                                // latestData[0].predict_score = 121
                                if (latestData[0].predict_score * 100 / data.ext.total_point >= 0.6) {
                                    VM1.isShowResRank = true;
                                    setTimeout(function () {
                                        if (latestData[0].resource_rank > 0) {
                                            VM1.selfRank = latestData[0].resource_rank;
                                        } else {
                                            VM1.selfRank = '--'
                                        }

                                        var dom2 = document.getElementById('growing-container-demo');
                                        $('#growing-container-demo').width($('.tab').width())
                                        drawGrowChart(dom2, pdates, resource_ranks, '排名')
                                    }, 0)

                                } else {
                                    VM1.isShowResRank = false;
                                    VM1.selfRank = '--'
                                }

                            }
                        })
                        VM1.loadingFlag = true;

                    },
                    error: function () {
                     //  console.log('chartData------------0请求trail失败')
                    }
                })
            },
            addClass: function () {

                window.addClass()
            },
            goDetailHelp: function () {
                setBackButtonStyle(1);
                window.location.href = './common-problem-detail.html?title=如何进入排名&statusHeight=' + statusHeight + "&titleHeight=" + titleHeight
            },
            drawScores: function (scores) {
                setTimeout(function () {
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
                var scale = 0;
                if (scores.total_score >= 0) {
                    scale = scores.total_score / scores.total_point;
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
                var isIos = window.isIos();
                var iosDp = $('html').attr('data-dpr')
                if (isIos) {
                    $('#app1 .small-div .no-prescore').css('fontSize', "0.64rem")
                    $('#app1 .small-div .score').css('fontSize', "0.96rem")
                    $('#app1 .small-div .total-score').css('fontSize', "0.373rem");
                    $('.out-on-off').css('fontSize', 18 * iosDp + 'px');
                } else {
                    $('.out-on-off').css('fontSize', '16px');
                }
            }, 0)
        }
        },
        beforeCreate: function () {
            // console.log('beforeCreate')
        },
        mounted: function () {
            var that = this;

            this.distributionCharts('class')
            this.distributionCharts('resource')
            this.selfGrow()
            $(".time-status-bar").height(statusHeight)
            $(".head-operate-container").height(titleHeight);
            $('#growthCon').css('padding-top', $('.all-bar').height())
            this.tabWidth = $('#app1 .title .text').width();
            this.tabLeft = $('#app1 .tab .title').width() / 2 - $('.tabs-active-bar').width() / 2;
            this.tabBarWidth = $('#app1 .tabs-active-bar').width();
        },
        computed: {
            tabBarStyle: function () {
                return {
                    '-webkit-transform': 'translateX(' + this.tabLeft + 'px)',
                    '-ms-transform': 'translateX(' + this.tabLeft + 'px)',
                    '-moz-transform': 'translateX(' + this.tabLeft + 'px)',
                    '-o-transform': 'translateX(' + this.tabLeft + 'px)',
                    transform: 'translateX(' + this.tabLeft + 'px)',
                    // width: this.tabWidth + 'px',
                }
            },

        },
        watch: {
            loadingFlag: function (newVal, oldVal) {
                if (newVal) {
                    this.isShowLoading = false;
                }
            }
        }
    })
    window.VM1 = VM1;
    // tab取消分布图
    $(document).on('click', '#app1 .tab .title', function (e) {
        VM1.contentChart = e.target.dataset.index;
        VM1.tabIndex = e.target.dataset.index;
        if ($(e.target).hasClass('text')) {
            VM1.tabLeft = $(e.target).parent('.title').position().left + $(e.target).parent('.title').width() / 2 - VM1.tabBarWidth / 2;
        } else {
            VM1.tabLeft = $(e.target).position().left + $(e.target).width() / 2 - VM1.tabBarWidth / 2;
        }
        if (VM1.tabIndex == 0) {
            VM1.distributionName = '班级'
            VM1.distributionCharts('class')
        } else {
            VM1.distributionCharts('resource')
        }

    })
    

}

// 学情图谱部分
function loadLearning(chartData) {
    $.ajax({
        url: API + '/point-service/account/info',
        type: 'POST',
        dataType: 'json',
        data: {
            id: account_id,
            resource_id: resource_id,
            token: token
        },
        success: function (data) {
         //  console.log('info-------------')
         //  console.log(JSON.stringify(data))
            if (data.code == 0 || data.code == -1) {
                var vacants = [true, true, true, true, true];
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
             //  console.log('chartData------------1')
             //  console.log(JSON.stringify(chartData))
                if(!chartData) {
                    chartData = {}
                }    
                if (chartData.data === null) {
                    chartData.data = []
                }
                var VM = new Vue({
                    el: '#app2',
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
                        tabLabelTextObj: tabLabelTextObj,
                        chartData: chartData,
                        isNotRadarData: false,
                    },
                    mounted: function () {
                        // $('.container').removeClass('hidePreScore');
                        this.drawRadarChart()
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
                        $('#growthCon, .child-tag').css('padding-top', $('.all-bar').height())
                        var tabWidth = $('#app2 .tab .item').width();
                        var tabLeft = $('#app2 .tab .item').eq(0).position().left;
                        var tabBarWidth = $('#app2 .tabs-active-bar').width()
                        var tabBarLeft = tabLeft + (tabWidth / 2) - (tabBarWidth / 2);
                        this.tabBarWidth = tabBarWidth;
                        this.tabWidth = tabWidth;
                        this.tabBarLeft = tabBarLeft;
                        $('#app2').find('.mask.hidden, .tag-demo-con.hidden').removeClass('hidden')
                    },
                    updated: function () {
                        var that = this;
                        if (this.childItems.length == 0) {
                            var isIos = window.isIos();
                            if (isIos) {
                                $('#app2 .ability-items .item .text').css('fontSize', '0.32rem');
                                $('#app2 .ability-items .item .text .unit').css('fontSize', '0.267rem');
                            }
                            if (this.vacants[this.tabIndex]) {
                                createCircle(this, this.data.tags);
                            } else {
                                this.loadingFlag = true
                            }
                            // 更新nowParentTagId绘制canvas
                            if (this.data && this.data.tags) {
                                var item = this.data.tags[this.nowParentTagId[0]];
                                if (this.nowParentTagId[0]) {
                                    if (!$('#app2 .big-canvas.canvas' + item.id).parents('.child-tag').hasClass('drawed')) {
                                        setTimeout(function () {
                                            that.setCSS();
                                            createCircle(that, item.child);
                                            createBigCircle(that, item);
                                            $('#app2 .big-canvas.canvas' + item.id).parents('.child-tag').addClass('active drawed');
                                        }, 0)
                                    }
                                }
                            }
                        } else {
                            var item = this.childItems[that.childItems.length - 1];
                            setTimeout(function () {
                                that.setCSS();
                                var childTag = $("#" + item.id);
                                $('#app2 .child-tag.active .child-tag-scroll').scrollTop(0);
                                childTag.children('.child-tag').addClass('active drawed');
                                createCircle(that, item.child);
                                createBigCircle(that, item);
                                this.growingLineHidden = true;
                            }, 0)
                        }
                    },
                    methods: {
                        drawRadarChart: function () {
                            var that = this;
                            var timer = null;
                            var titleArr = this.titleArr;
                            var chartDom = document.getElementById('radar-chart');
                            if (chartDom) {
                                timer = null;
                                var type = this.data && this.data.ext ? this.data.ext.open_type : 0;
                                var rate = computeOpenType(type);
                                var color_start = '#16F27C', color_end = '#0AA8F0';
                                // console.log(VM1.scores)
                                if (VM1.scores) {
                                    var r = VM1.scores.total_score / VM1.scores.total_point;
                                    if (r < 0.8 && r >= 0.6) {
                                        color_start = '#F29211'
                                        color_end = '#F4DC1A'
                                    } else if (r < 60) {
                                        color_start = '#F5891E'
                                        color_end = '#F72A56'
                                    }
                                }
                                var data = null;
                                var indicator = [];
                                if (this.chartData && this.chartData.data && this.chartData.data.length > 0) {
                                    data = this.chartData.data[0];
                                    indicator = titleArr.map(function (item, index) {
                                        var obj = {};
                                        obj.text = item;
                                        obj.max = 1;
                                        obj.type = openTypeTextArr[index];
                                        switch (index) {
                                            case 0:
                                                obj.value = data.listen_rate;
                                                break
                                            case 1:
                                                obj.value = data.speak_rate;
                                                break
                                            case 2:
                                                obj.value = data.read_rate;
                                                break
                                            case 3:
                                                obj.value = data.write_rate;
                                                break
                                            case 4:
                                                obj.value = data.view_rate;
                                                break
                                            default:
                                                break
                                        }
                                        return obj
                                    })
                                    indicator = indicator.filter(function (item) {
                                        return rate[item.type] === 1
                                    })
                                }


                                if (indicator.length === 0 || data === null) {
                                    // 没有数据的情况，需要把提示文字展示出来
                                    this.isNotRadarData = true;
                                    indicator = titleArr.map(function (item, index) {
                                        var obj = {};
                                        obj.text = item;
                                        obj.max = 1;
                                        obj.type = openTypeTextArr[index];
                                        obj.value = -1;
                                        return obj
                                    })
                                }
                                if (indicator[0].type == openTypeTextArr[0]) {
                                    indicator.push(indicator.shift())
                                }
                                indicator.reverse();
                                var option = {
                                    symbolSize: 10 * dps,
                                    backgroundColor: '#fff',
                                    legend: {
                                        padding: 0,
                                    },
                                    radar: {
                                        shape: 'circle',
                                        indicator: indicator,
                                        center: ['50%', '50%'],
                                        axisLine: {
                                            lineStyle: {
                                                color: '#E6E6E6',
                                                type: 'dashed'
                                            }
                                        },
                                        radius: '70%',
                                        startAngle: 90,
                                        splitNumber: 5,
                                        splitLine: {
                                            lineStyle: {
                                                color: '#E6E6E6',
                                            }
                                        },
                                        splitArea: {
                                            areaStyle: {
                                                color: ['#fff', '#FCFCFC', '#fff', '#FCFCFC', '#fff']
                                            }
                                        },
                                        shape: 'circle',
                                        name: {
                                            formatter: function (value, indicator) {
                                                var val = value;
                                                if (indicator.value != -1) {
                                                    val += '(' + Math.floor(indicator.value * 100) + '%)'
                                                }
                                                return val
                                            },
                                            textStyle: {
                                                color: '#979797',
                                                fontSize: 20,
                                            }
                                        },
                                        nameGap: 20,
                                    },
                                    series: [
                                        {
                                            type: 'radar',
                                            itemStyle: {
                                                opacity: 0
                                            },
                                            lineStyle: {
                                                width: 0,
                                            },
                                            areaStyle: {
                                                color: {
                                                    type: 'linear',
                                                    x: 0,
                                                    y: 0,
                                                    x2: 1,
                                                    y22: 1,
                                                    colorStops: [{
                                                        offset: 0, color: color_start // 0% 处的颜色
                                                    }, {
                                                        offset: 1, color: color_end // 100% 处的颜色
                                                    }],
                                                    global: false // 缺省为 false
                                                }
                                            },
                                            data: [{
                                                value: indicator.map(function (item) { return item.value === -1 ? 0 : item.value })
                                            }]
                                        }
                                    ]
                                };
                                var radarChart = echarts.init(chartDom);
                                radarChart.setOption(option)
                            } else {
                                timer = setTimeout(function () {
                                    that.drawRadarChart()
                                }, 500)
                            }
                        },
                        setCSS: function () {
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
                        tabAddress: function (e) {
                            if (this.tabIndex != e.target.dataset.index) {
                                this.loadingFlag = false;
                                this.tabIndex = e.target.dataset.index;
                                sessionStorage.setItem('tabAddressIndex', e.target.dataset.index)
                                var selfLeft = $(e.target).position().left;
                                this.tabBarLeft = selfLeft + (this.tabWidth - this.tabBarWidth) / 2;
                                $('.growing-container1').width($('#app2 .tab').width());
                                if (growChart) {
                                    growChart.dispatchAction({
                                        type: 'hideTip'
                                    })
                                }
                                setTimeout(function () {
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
                        goHelp: function () {
                            setBackButtonStyle(1);
                            window.location.href = './common-problem-detail.html?title=能力标签得分率&statusHeight=' + statusHeight + "&titleHeight=" + titleHeight
                        },
                        getItemScore: function (item) {
                            var that = this;
                            var nowOpenText = openTypeTextArr[that.tabIndex];
                            var openStatu = computeOpenType(item.open_type);
                            var score = 0;
                            if (openStatu[nowOpenText] && that.data.data) {
                                var label = JSON.parse(that.data.data[nowOpenText.replace('Open', '_label')].replace(/\'/g, "\""));
                                if (label[item.id]) {
                                    score = Math.round(label[item.id] * 100)
                                }
                            }
                            return score
                        },
                        isOpenTag: function (item) {
                            var that = this;

                            var nowOpenText = openTypeTextArr[that.tabIndex];
                            var openStatu = computeOpenType(item.open_type);
                            if (openStatu[nowOpenText]) {
                                return true
                            }
                            return false
                        },
                        getTagLevelTitle: function (tag) {
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
                        subStrName: function (string) {
                            if (string.length > 5) {
                                return string.substr(0, 5) + '\n' + string.substr(5)
                            } else {
                                return string
                            }
                        },
                        goNextTag: function (e, item) {
                            var dom = $(e.currentTarget);
                            var tags = this.data.tags;
                            var nowParentTagId = this.nowParentTagId;
                            var childTag = dom.siblings(".child-tag-contain").children('.child-tag');
                            if (item.id != nowParentTagId[0] && tags[item.id]) {
                                this.nowParentTagId[0] = item.id;
                            }
                            this.tagsArrForname.push(item);
                            var l = $('#app2 .child-tag.active').length;
                            if (!childTag.hasClass('drawed') && item.id != nowParentTagId[0] && nowParentTagId[0]) {
                                this.nowParentTagId.push(item.id);
                                this.childItems.push(item);
                            } else {
                                this.growingLineHidden = true;
                                $('#app2 .child-tag.active .child-tag-scroll').scrollTop(0);
                                childTag.addClass('active drawed');
                            }
                            $('.growth-tab').hide();
                        },
                        showChildDom: function (tag) {
                            return this.childItems.filter(function (item) {
                                return item.id == tag.id
                            }).length > 0
                        },
                        showGrowChartTooltip: function () {
                            growChart.dispatchAction({
                                type: 'showTip',
                                seriesIndex: 0, //第几条series
                                dataIndex: pda.length - 1, //第几个tooltip
                            });
                        },
                        showDemo: function (tag) {
                            this.tagDemoText = tag.demo
                        },
                        hideDemo: function () {
                            this.tagDemoText = ''
                        }
                    },
                    watch: {
                        loadingFlag: function (newVal, oldVal) {
                            if (newVal) {
                                this.isShowLoading = false;
                            } else {
                                this.isShowLoading = true;
                            }
                        }
                    },
                    computed: {
                        tabLabelText: function () {
                            var index = this.tabIndex;
                            return this.tabLabelTextObj[tabLabelKeyArr[index]]
                        },
                        tabBarStyle: function () {
                            return {
                                '-webkit-transform': 'translateX(' + this.tabBarLeft + 'px)',
                                '-ms-transform': 'translateX(' + this.tabBarLeft + 'px)',
                                '-moz-transform': 'translateX(' + this.tabBarLeft + 'px)',
                                '-o-transform': 'translateX(' + this.tabBarLeft + 'px)',
                                transform: 'translateX(' + this.tabBarLeft + 'px)',
                            }
                        },
                        title: function () {
                            return this.titleArr[this.tabIndex]
                        }
                    },
                    filters: {
                        subStr: function (string) {
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

                    chartData.data.forEach(function (item, index) {
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
                    pdates.forEach(function (item, index) {
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
                    $('.growing-container1').width($('#app2 .tab').width());
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
                // 判断是否有tab的点击数据
                var growthTabIndex = parseInt(sessionStorage.getItem('growthTabIndex')) || 0;
                var tabAddressIndex = parseInt(sessionStorage.getItem('tabAddressIndex')) || 0;
                $('.growth-tab .item').eq(growthTabIndex).click()
                $('#app2 .tab .item').eq(tabAddressIndex).click()
            } else {
                setBackButtonStyle(1)
                $('#loading-fail-2').removeClass('hidePreScore');
             //  console.log('----info code')
            }
        },
        error: function (err) {
            setBackButtonStyle(1)
            $('#loading-fail-2').removeClass('hidePreScore');
         //  console.log('------------0请求info失败')
        }
    })
}