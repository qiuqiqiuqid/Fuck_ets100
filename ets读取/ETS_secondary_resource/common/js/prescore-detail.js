// $(function() {
var dps = document.documentElement.clientWidth / 375;
getHeight();
var hideJoinClass = GetQueryString('hideJoinClass');
if(hideJoinClass){
    $('.add-class').hide();
}
// 页面重新加载
$(document).on('click', '#loading-fail', function() {
    window.location.reload()
})
// 个人成长轨迹图 班级排名轨迹 省级排名轨迹 函数
function growChart(dom, dates, scores, yName,max) {
    if (yName == '成绩') {
        var inverse = false;
        var origin = 'start';
         var min=null;
         var max=max
    } else if(yName=='排名'){
        var inverse = true;
        var origin = 'end';
        var min="dataMin";
        var max=null

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
            formatter: function(params) {
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
                interval: function(index, value) {
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
                formatter:function(val,index){
                    // if(Number.isInteger(val)){
                    //     return val
                    // }
                    return val%1==0 ? val : ""
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
            max:max
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
        var offsetScale = 1 - (coord[1] / VM.scores.total_point);
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
                formatter: function() {
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
    return function(obj1, obj2) {
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