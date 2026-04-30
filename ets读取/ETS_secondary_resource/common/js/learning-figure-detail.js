    var dps = document.documentElement.clientWidth / 375;
    var dpr = window.devicePixelRatio;
    getHeight();
    var growChart = null;
    // 图表
    function makeCharts(dom, dataX, dataY) {
        growChart = echarts.init(dom);
        // 班级预测分图表
        var option = {
            grid: {
                left: 0,
                right: 5*dps ,
                top: 10 * dps,
                bottom: 0,
                containLabel: true
            },
            tooltip: {
                trigger: 'axis',
                formatter: function(params) {
                    return "日期：" + params[0].axisValue + "<br/> 得分率：" + Math.round(params[0].data*100)+"%"
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
                    interval: function(index, value) {
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
                    formatter: function(val, index) {
                        return Math.round((val * 100)) + "%"
                    }
                },
                splitLine: {
                    lineStyle: {
                        color: '#e6e6e6',
                        width: 0.5 * dps
                    }
                },
                max:1
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
        setTimeout(function(){
            growChart.dispatchAction({
              type: 'showTip',
              seriesIndex:0 ,//第几条series
              dataIndex: dataX.length-1,//第几个tooltip
            });
        },0)
        if (option && typeof option === "object") {
            growChart.setOption(option, true);
            growChart.resize();
        }
    }


    // 能力标签canvas
    function createCircle(that,tags) {
        var index = that.tabIndex;
        // var tags = that.data.tags;
        // // 绘制能力标签得分率圆环
        var nowOpenText = openTypeTextArr[that.tabIndex];
        var color = '';
        for(var i in tags){
            var tag = tags[i];
            var openStatu = computeOpenType(tag.open_type);
            if(openStatu[nowOpenText]){
                var label = JSON.parse(that.data.data[nowOpenText.replace('Open','_label')].replace(/\'/g, "\""));
                var score = label[i] ? label[i] : 0;
                if (score <= 0.6) {
                    color = '#ff6268'
                } else if (score <= 0.8) {
                    color = '#ffa600'
                } else {
                    color = '#3ad56f'
                }
                var canvas = $('.content').eq(0).find(".small-canvas.canvas" + i)[0];
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
                context.arc(x, y, 25.5 * dps, -Math.PI / 2, -Math.PI / 2 + (score)  * 2 * Math.PI, false);
                context.stroke();
                context.closePath();
                context.restore();
                context=null;
            }
        }
        that.loadingFlag = true;
    }

    function createBigCircle(that,tag) {
        var index = that.tabIndex;
        // var tags = that.data.tags;
        // // 绘制能力标签得分率圆环
        var nowOpenText = openTypeTextArr[that.tabIndex];
        var color = '';
        var openStatu = computeOpenType(tag.open_type);
        var i = tag.id;
        if(openStatu[nowOpenText]){
            var label = JSON.parse(that.data.data[nowOpenText.replace('Open','_label')].replace(/\'/g, "\""));
            var score = label[i] ? label[i] : 0;
            if (score <= 0.6) {
                color = '#ff6268'
            } else if (score <= 0.8) {
                color = '#ffa600'
            } else {
                color = '#3ad56f'
            }
            var canvas = $('.content').eq(0).find(".big-canvas.canvas" + i)[0];
            var width = $(".big-canvas").width();
            var height = $(".big-canvas").height();
            var context = canvas.getContext('2d');
            canvas.width = width  < document.documentElement.clientWidth ? width : 150;
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
            context.arc(x, y, r, -Math.PI / 2, -Math.PI / 2 + (score)  * 2 * Math.PI, false);
            context.stroke();
            context.closePath();
            context.restore();
            context=null;
        }
        that.loadingFlag = true;
    }

    // 页面重新加载
    $(document).on('click', '#loading-fail', function() {
        window.location.reload()
    })