var navigatorUA = navigator.userAgent.toLowerCase();
var etsCommon = {
    getReadingAloudCount: 0,
    userAgent: navigatorUA,
    system: /iphone|ipad|ipod/.test(navigatorUA) ? 1 : (/android/.test(navigatorUA) ? 2 : 8),
    bj_title_text: {
        '1': '听下面一段对话，回答第1小题。',
				'2': '听下面一段对话，回答第2小题。',
    		'3': '听下面一段对话，回答第3小题。',
    		'4': '听下面一段对话，回答第4小题。',
    		'5': '听下面一段对话，回答第5小题。',
    		'6': '下面，请准备录音。听到录音提示音后，在10秒钟内完成作答。',
    		'7': '现在你有1分钟的时间浏览提示信息。',
    		'8': '下面，请在90秒钟内将所缺信息输入到指定的答题区域。',
    		'9': '请再听一遍短文，完成转述。',
    		'10': '下面，你有2分钟的时间做转述准备。转述的开头已给出。',
    		'11': '下面，请准备录音。听到录音提示音后，在2分钟内完成转述。',
    		'12': '先浏览短文，然后完成朗读。',
    		'13': '现在，你有90秒钟的时间浏览短文并做录音准备',
    		'14': '下面请准备录音。听到录音提示音后，在2分钟内完成朗读。'
    },

    change_title_text: function(num, id) {
        templateApp[id] = this.bj_title_text[num];
        // $('.'+id).text(bj_title_text[num]);
    },
    // 点击显示大图
    showBigImg: function(img_url) {
        var ua = navigator.userAgent.toLowerCase();
        if (this.system == 1) {
            /*ios*/
            location.href = "ets://show_big_img?imgUrl=" + img_url;
        } else if (/android/.test(ua)) {
            /*android*/
            window.webInteraction.showBigImg(img_url + "");
        }
    },
    // 格式化数据
    formatJSONString: function(str) {
        var data = '';
        if (typeof str === 'string') {
            str = str.replace(/\r\n|\r|\n|↵|\\r\\n|\\r|\\n/g, '<br />')
                .replace(/[\u0000\ufeff]/g, '')
            try {
                data = JSON.parse(str)
            } catch (err) {
                console.log("解析试题json数据失败")
            }
        } else {
            data = str
        }
        return data
    },
    // 获取分数颜色
    getScoreTextColor: function(getScore, allScore) {
        var ration = getScore / allScore;
        var color = '';
        if (ration >= 0 && ration < 0.6) {
            color = "color_red"
        } else if (ration >= 0.6 && ration < 0.8) {
            color = "color_yellow"
        } else if (ration >= 0.8 && ration <= 1) {
            color = "color_green"
        };
        return color
    },

    // 设置做题页面数据
    setTemplateData: function(str, url, randomOrder) {
        var data = this.formatJSONString(str);
        var content_img = [];
        for (var i in data) {
            if(data[i].classname){
                var classname = data[i].classname;
                var code_value = data[i].code_value;
                // var code_id = data[i].code_id;
                // var code_type = data[i].code_type;
                if (classname == 'content_img') {
                    content_img.push(code_value);
                } else if (classname == 'content_choose' || classname == 'content_choose2' || classname == 'content_fill_answer') {
                    if (typeof code_value === 'string') {
                        templateApp[classname] = JSON.parse(code_value);
                    } else {
                        templateApp[classname] = code_value;
                    }
                    // 随机打乱选项顺序
                    if((classname == 'content_choose' || classname == 'content_choose2') ) {
                        if(randomOrder == 1 && templateApp[classname].choose_type!='1') {
                            if(templateApp[classname].xtlist) {
                                templateApp[classname].xtlist.forEach(function(item, index) {
                                    if(item.xxlist) {
                                        var hasAdd = []; // 已经用过的选项
                                        var xx_mc_arr = item.xxlist.map(function(xx_mc) {
                                            return xx_mc.xx_mc
                                        })
                                        item.xxlist.forEach(function(choose){
                                            var random = Math.floor(Math.random() * xx_mc_arr.length);
                                            while(hasAdd.indexOf(random) > -1) {
                                                random = Math.floor(Math.random() * xx_mc_arr.length);
                                            }
                                            hasAdd.push(random);
                                            templateApp.$set(choose, 'xx_mc_text', xx_mc_arr[random])
                                        })
                                        item.xxlist.sort(function(a,b) {
                                            if(a.xx_mc_text > b.xx_mc_text) {
                                                return 1
                                            }
                                            if(a.xx_mc_text < b.xx_mc_text) {
                                                return -1
                                            }
                                            return 0
                                        })
                                    }
                                })
                            }
                        }else {
                            if(templateApp[classname].xtlist) {
                                templateApp[classname].xtlist.forEach(function(item, index) {
                                    if(item.xxlist) {
                                        item.xxlist.forEach(function(choose){
                                            templateApp.$set(choose, 'xx_mc_text', choose.xx_mc)
                                        })
                                    }
                                })
                            }
                        }
                        
                    }
                } else if(classname == 'words_con'||classname == 'words_tla'||classname == 'words_mark'){
                    templateApp.words = data;
                    templateApp[classname] = code_value;
                } else if(classname=='read_con'||classname=='read_sen'||classname=='read_mark'||classname=='read_tla'||classname=='read_title'||classname=='read_titlesrc'){
                    templateApp.read = data;
                    // 处理图片 src 问题
                    if(classname=='read_title'){
                        if(code_value.indexOf('src')>-1){
                            var src_index = code_value.indexOf('src');
                            // html 前半段, 不包含img
                            var str1 = code_value.slice(0,src_index);
                            // html 图片部分
                            var str2 = code_value.slice(src_index, src_index+49);
                            str2 = 'src=\"images/ans_record_icon.png\"';
                            // html 后半段
                            var str3 = code_value.slice(src_index+49, code_value.length);
                            // 多增加一个 data-id ，用于播放的时候替换图片
                            var str4 = ' data-id="1"'
                            code_value = str1 + str2 + str4 + str3;
                        }
                    }
                    templateApp[classname] = code_value;
                } else if(classname == 'spell' || classname== 'spell_org' || classname == 'spell_info'){
                    templateApp.words = data;
                    if(classname == 'spell'){
                        templateApp['words_con'] = code_value;
                    }else if(classname == 'spell_org'){
                        templateApp['spell_txt_org'] = code_value;
                    }else if(classname == 'spell_info'){
                        templateApp['spell_tla'] = code_value;
                    }
                } else if(classname=='sen_con' || classname=='sen_tla' || classname=='phrase_con'){
                    templateApp.sentence = data;
                    templateApp[classname] = code_value;
                } else if(classname=='essay_con'||classname=='essay_tla'||classname=='essay_annota'||classname=='essay_intro'){
                    templateApp.essay = data;
                    templateApp[classname] = code_value;
                } else if(classname=='gap_title'||classname=='gap_imgcon'||classname=='gap_answer'||classname=='gap_mixture'||classname=='gap_comitcon'){
                    templateApp.content_gap = data;
                    templateApp[classname] = code_value;
                } else if(classname=='choose_title'||classname=='choose_con'||classname=='choose_comitcon'){
                    templateApp.choose = data;
                    if (classname=='choose_con' && typeof code_value === 'string') {
                        templateApp[classname] = JSON.parse(code_value);
                    } else {
                        templateApp[classname] = code_value;
                    }
                } else {
                    templateApp[classname] = code_value;
                }
            }
        }
        templateApp.content_img = content_img;
        templateApp.materialUrl = url ? url : '';
        this.showContainer();
    },
    showContainer: function() {
        $(".container").show();
    },
    // 多维度展示top按钮
    setDwdContainer: function(bool) {
        templateApp.isDwdContainer = bool;
    },
    // 设置答案页面做题数据
    setQuestions: function(str, url, option) {
        var data = this.formatJSONString(str);
        var params = option || {}
        if(params.diagnosisData) {
            this.setRepeatQuestionDiagnosisData(params.diagnosisData, params.keypoint);
        }
        // 如果有诊断进度，设置诊断进度
        if(params.diagnosisProgress) {
            this.setRepeatDiagnosisStatus(1)
            this.setRepeatQuestionDiagnosisProgress(params.diagnosisProgress);
        }
        templateApp.diagnosisButtonVisible = params.diagnosisButtonVisible || false;
        templateApp.isRepeatQuestion = !!params.isRepeatQuestion;
        templateApp.studentOriginText = params.studentOriginText || '';
        // 二期：恢复诊断状态（退出再进入时 Native 传入当前诊断状态）
        if(typeof params.repeatDiagnosisStatus !== 'undefined') {
            templateApp.repeatDiagnosisStatus = params.repeatDiagnosisStatus;
        }
        // 二期：已有诊断结果（完整JSON），直接渲染（非流式）
        if(params.retellDiagnosisData && templateApp.loadRetellDiagnosisData) {
            templateApp.loadRetellDiagnosisData(params.retellDiagnosisData);
        }
        templateApp.questions = data;
        templateApp.materialUrl = url ? url : '';
    },
    showScore: function(maxScore, currScore, isOnTop) {
        if(typeof isOnTop === 'undefined') {
            var isOnTop = true
        }
        templateApp.getScore = currScore;
        templateApp.allScore = maxScore;
        templateApp.isScoreOnTop = isOnTop;
        if(isOnTop) {
            $('.result_con').show();
        }else {
            $('.new-result-con').show()
        }
    },
    setScore: function(maxScore, currScore, isOnTop) {
        if(typeof isOnTop === 'undefined') {
            var isOnTop = true
        }
        templateApp.getScore = currScore;
        templateApp.allScore = maxScore;
        templateApp.isScoreOnTop = isOnTop;
        if(isOnTop) {
            $('.result_con').show();
        }else {
            $('.new-result-con').show()
        }
    },
    /****设置每道题的得分，不适用于长对话理解**/
    /*
     **     number: 题号,
     ** score:  用户分数,
     **     all_score:  总分
     ** stander_ans: 正确答案,如果不是选择题，则不传此字段
     ** user_ans: 用户答案，如果不是选择题，则不传此字段
     ** 例如：  短对话理解时---- [{number:"42737_1",score:0,all_score:5,stander_ans:"A",user_ans:"B"},{}]
     **         其他只需要设置分数的： [{number:"42737_1",score:3,all_score:5}]
     */
    setScoreAnswer: function(arr) {
        if ($('.record_icon').length == 0) {
            var self = this;
            setTimeout(function() {
                self.setScoreAnswer(arr)
            }, 100)
        } else {
            var arr = typeof arr === 'string' ? JSON.parse(arr) : arr;
            $(arr).each(function(index, item) {
                var number = item.number,
                    score = item.score,
                    all_score = item.all_score,
                    stander_ans = item.stander_ans,
                    user_ans = item.user_ans;
                var q_score = $("#" + number).parent();
                q_score.find(".q_score_con").text('得分：' + score + "分/" + all_score + "分");
                var ration = score / all_score;
                if (ration >= 0 && ration < 0.6) {
                    q_score[0].className = "q_score red";
                } else if (ration >= 0.6 && ration < 0.8) {
                    q_score[0].className = "q_score yellow";
                } else if (ration >= 0.8 && ration <= 1) {
                    q_score[0].className = "q_score green";
                };
            })
        }
    },
    setFillScoreAnswer: function(arr) {
        if ($('.fill_word_input').length == 0) {
            var self = this;
            setTimeout(function() {
                self.setFillScoreAnswer(arr)
            }, 100)
        } else {
            var arr = typeof arr === 'string' ? JSON.parse(arr) : arr;
            $(arr).each(function(index, item) {
                var number = item.number,
                    score = item.score,
                    all_score = item.all_score,
                    stander_ans = item.stander_ans,
                    user_ans = item.user_ans;
                var q_score = $("span[data-id=" + number+"]").parent();
                q_score.find(".q_score_con").text('得分：' + score + "分/" + all_score + "分");
                var ration = score / all_score;
                if (ration >= 0 && ration < 0.6) {
                    q_score[0].className = "q_score red";
                } else if (ration >= 0.6 && ration < 0.8) {
                    q_score[0].className = "q_score yellow";
                } else if (ration >= 0.8 && ration <= 1) {
                    q_score[0].className = "q_score green";
                };
            })
        }
    },
    /*模考播放完录音后按钮设置回白色背景*/
    reset_mkButtonView: function(str) {
        var that = $('.audio_con .' + str);
        var src = '';
        that.removeClass("playing");
        if (str == 'stander_record') {
            src = 'images/ans_record_icon_blue.png';
            that.find('span').text('原音');
        } else {
            src = 'images/ans_my_record_blue.png';
            that.find('span').text('录音');
        };
        that.css({ "background-color": '#fff'});
        that.find('img').attr('src', src);
    },
    // 设置多维度分数
    // setScoreDetail: function(str) {
    //     var arr = typeof str === 'string' ? JSON.parse(str) : str;
    //     if (arr.length == 4) {
    //         $(".dwd_item").removeClass('none').parent().css({ "display": "block", "margin": "0.375rem 0.6rem", "width": "initial" });
    //     }
    //     for (var i = 0; i < arr.length; i++) {
    //         var item = arr[i];
    //         $("#" + item.id).find(".dwd_flu").text(item.score);
    //     }
    //     $('.dwd_score').removeClass('none');
    // },
    // 设置多维度分数
    setScoreDetail: function(str) {
        var arr = typeof str === 'string' ? JSON.parse(str) : str;
        if (arr.length == 4) {
            $(".dwd-item").removeClass('none')
        }else {
            $(".dwd-item").eq(3).remove()
        }
        for (var i = 0; i < arr.length; i++) {
            var item = arr[i];
            $("#" + item.id).find(".num").text(item.score);
        }
        $('.dwd-analysis').removeClass('none');
    },
    // 多个音频播放的时候重置gif为png
    reset_gifToPng: function() {
        var playing = $('.playing');
        if (playing.length > 0) {
            $(playing).each(function(index, item) {
                playing.removeClass('playing');
            })
        }
    },
    // 做题页用的重置gif为png
    newReset_gifToPng: function(){
        var playing = $('.playing');
        if (playing.length > 0) {
            $(playing).each(function(index, item) {
                if (playing.hasClass('sample_audio')) {
                    playing.removeClass('playing');
                    return
                }
                if ($(item).is('img')) {
                    var playing_src = $(item).attr('src');
                    // var index = playing_src.lastIndexOf('/');
                    var png_src = playing_src.replace('.gif', '.png');
                    $(item).attr('src', png_src).removeClass('playing')
                } else {
                    var playing_src = $(item).find("img").attr('src');
                    // var index = playing_src.lastIndexOf('/');
                    var png_src = '';
                    if($(item).parent().hasClass('audio_con')){
                        png_src = playing_src.replace('_onP.gif', '.png');
                    }else{
                        png_src = playing_src.replace('.gif', '.png');
                    }
                    $(item).removeClass('playing').find("img").attr('src', png_src);
                    if($(item).parent().hasClass('audio_con')){
                        $(item).css({'background-color':"#fff", "color": 'rgb(58, 213, 111)'})
                    }
                }
            })
        }
    },
    // 获取html内容
    getReadingAloud: function() {
        var content = $(".recordText_con").html();
        if(content == null){
            if(etsCommon.getReadingAloudCount < 5){
                setTimeout(function(){
                    etsCommon.getReadingAloudCount++;
                    etsCommon.getReadingAloud();
                }, 50)
            }
        }else{
            content = content.trim();
            window.webInteraction.getReadingAloudContent(content + "");
        }
    },
    setReadingAloud: function(html) {
        $('.test_con .recordText_con').html(html);
    },
    /*****************************/
    /***********选择题*************/
    /*****************************/
    numtoa: function(str) {
        var numInt = parseInt(str);
        if (!isNaN(numInt) && numInt > -1 && numInt < 27) {
            var newNumInt = numInt + "A".charCodeAt(0);
            var retVal = String.fromCharCode(newNumInt);
            return retVal;
        }
        return false;
    },

    atonum: function(str) {
        if (str && $.trim(str).length == 1) {
            str = str.toUpperCase();
            var retVal = str.charCodeAt(0) - 'A'.charCodeAt(0) + 1;
            if (retVal > 0 && retVal < 27) {
                return retVal;
            }
        }
        return false;
    },

    setBackgroundColor: function(id) {
        $("#" + id).addClass("question_on").siblings().removeClass("question_on");
    },


    scrollTo: function(id) {
        var h = $("#" + id).offset().top;
        $(".container").scrollTop(h);
    },

    setChoice: function(str) {
        var arr = str.split(",");
        $(arr).each(function(index, item) {
            if ($("#" + item).hasClass("choose1")) {
                $("#" + item).addClass("choose1_selected").siblings().removeClass("choose1_selected");
            } else {
                $("#" + item).addClass("choose2_selected").siblings().removeClass("choose2_selected");
            }
        });
    },

    getChoice: function(childId) {
        var that = this;
        var arr = [];
        if ($(".choose1").length !== 0) {
            $(".choose1_selected").each(function(index, item) {
                var i = $(item).index();
                var a = {};
                a.id = $(this).parent().parent().parent().attr("id");
                a.answer = $(this).attr('data-real-answer');
                arr.push(a);
            })
        }
        if ($(".choose2").length !== 0) {
            $(".choose2_selected").each(function(index, item) {
                var i = $(item).index();
                var a = {};
                a.id = $(this).parent().parent().attr("id");
                a.answer = $(this).attr('data-real-answer');
                arr.push(a);
            })
        };
        console.log(arr);
        if (this.system == 1) {
            location.href = "ets://get_choice?arr=" + JSON.stringify(arr) + "&childId=" + childId;
        } else if (this.system == 2) {
            window.webInteraction.getAnswerList(JSON.stringify(arr) + "", childId + "");
        }
    },

    kttb_getAllInputAnswer: function(posi) {
        etsCommon.kttb_getAllInputAnswer(posi);
    },

    // 获取用户答案
    kttb_getAllInputAnswer: function(posi) {
        var all_l = $('input').length,
            n = 0;
        var ans_arr = [];
        $('input').each(function(index, item) {
            var val = $(item).val().trim();
            var id = $(item).attr('id');
            var o = {};
            o.answer = val;
            o.id = id;
            ans_arr.push(o);
            if (val) {
                n++;
            };
        })
        var ansStr = JSON.stringify(ans_arr);
        var ua = navigator.userAgent.toLowerCase();
        if (/iphone|ipad|ipod/.test(ua)) {
            location.href = "ets://get_word_list?arr=" + ansStr + "&questCount=" + all_l + "&answerCount=" + n;
        } else if (/android/.test(ua)) {
            window.webInteraction.getAllInputAnswer(ansStr, all_l, n, posi);
        }
    },

    // 选择题
    kttb_getChoice: function(childId, position) {
        var arr = [],
            all_l, n = 0;
        if ($(".choose1").length !== 0) {
            $(".choose1_selected").each(function(index, item) {
                n++;
                var i = $(item).index();
                var a = {};
                a.id = $(this).parent().parent().parent().attr("id");
                a.answer = etsCommon.numtoa(i);
                arr.push(a);
            })
        }
        if ($(".choose2").length !== 0) {
            $(".choose2_selected").each(function(index, item) {
                n++;
                var i = $(item).index();
                var a = {};
                a.id = $(this).parent().parent().attr("id");
                a.answer = etsCommon.numtoa(i);
                arr.push(a);
            })
        };
        all_l = $(".question1").length + $('.question2').length;

        // console.log(arr) ;

        var ua = navigator.userAgent.toLowerCase();
        if (/iphone|ipad|ipod/.test(ua)) {
            location.href = "ets://get_choice?arr=" + JSON.stringify(arr) + "&childId=" + childId + "&questCount=" + all_l + "&answerCount=" + n;
        } else if (/android/.test(ua)) {
            window.webInteraction.kttb_getChoice(JSON.stringify(arr) + "", childId + "", all_l, n, position);
        }
    },
    
    // 设置选择题答案
    setChooseAnswer: function(arr) {
        var that = this;
        if ($('.choose1, .choose2').length > 0) {
            etsCommon.setChooseAnswerFn(arr);
        } else {
            setTimeout(function() {
                etsCommon.setChooseAnswer(arr);
            }, 200)
        }
    },

    setChooseAnswerFn: function(arr) {
        $(arr).each(function(index, item) {
            var number = item.number,
                score = item.score,
                all_score = item.all_score,
                stander_ans = item.stander_ans,
                user_ans = item.user_ans;
            if ($('#' + number).hasClass('question1')) {
                //选图
                $('#' + number).find('.q_score_con').text('得分：' + score + "分/" + all_score + "分")
                if (stander_ans == user_ans) {
                    $('#' + number).find('.q_score_con').parent()[0].className = "green";
                    $("#" + number).find(".question1_title").css("color", "#43cb75");
                    var num = etsCommon.atonum(stander_ans) - 1;
                    $($("#" + number).find(".choose1")[num]).addClass("rightPic").find('.choose1_input').addClass('right');
                } else {
                    $("#" + number).find(".question1_title").css("color", "#e65151");
                    var r_num = etsCommon.atonum(stander_ans) - 1;
                    var u_num = etsCommon.atonum(user_ans) - 1;
                    $($("#" + number).find(".choose1")[r_num]).find('.choose1_input').removeClass('error').addClass('right');
                    $($("#" + number).find(".choose1")[u_num]).addClass("errorPic");
                };
            } else if ($('#' + number).hasClass('question2') || $('#' + number).parent().hasClass('question2')) {
                //短对话
                var q_score = $("#" + number);
                q_score.find(".q_score_con").text('得分：' + score + "分/" + all_score + "分");
                if (stander_ans == user_ans) {
                    $('#' + number).find('.q_score_con').parent().removeClass('red').addClass('green');
                };
                var choose2 = $("#" + number + " .choose2");
                var right_num = etsCommon.atonum(stander_ans) - 1;
                $(choose2[right_num])[0].className = "choose2 rightCh green";
                if (stander_ans != user_ans && user_ans !== '') {
                    var user_num = etsCommon.atonum(user_ans) - 1;
                    $(choose2[user_num])[0].className = "choose2 errorCh red";
                };
            }
        })
    },
    /*****************************/
    /**********选择题结束**********/
    /*****************************/

    /******新试题结构用到的方法*****/
    /*****************************/
    // 让所有input控件失去焦点
    kttb_input_blur: function(){
        $('input').blur();
    },

    // 获取听说同步的单词内容
    kttb_getWordHtml: function() {
        var html = $(".word_txt").html();
        window.webInteract.getWordHtml(html);
    },

    // 设置听说同步的单词着色文本
    kttb_setWordHtml: function(html) {
        $(".word_txt").html(html);
    },

    // 设置单词
    kttb_setWord: function(word, b) {
        $('.write_input_con').hide();
        $('.write_result_text').text(word).removeClass('green red').parent().show();
        $('.next_icon').text('').removeClass('next_icon_right next_icon_error');
        if (b) {
            $('.write_result_text').addClass('green');
            $('.next_icon').addClass('next_icon_right');
        } else {
            $('.write_result_text').addClass('red');
            $('.next_icon').addClass('next_icon_error');
        };
        etsCommon.showOrHideAnalysis();
    },

    // 判断是否有答案解析
    showOrHideAnalysis: function() {
        var ana = $('.analysis');
        var text = ana.text().trim();
        if (text === '答案解析：' || text === '答案解析') {
            ana.hide();
        } else {
            ana.show();
        };
    },

    // 判断题提交后设置答案
    // user_answer:   0  或   1
    // b:   true   或   false ,代表对错
    kttb_setJudge: function(user_answer, b) {
        if (b) {
            $('.button').eq(user_answer).removeClass('button_right button_error button_yes button_no button_disabled').addClass('button_right');
        } else {
            $('.button').eq(user_answer).removeClass('button_right button_error button_yes button_no button_disabled').addClass('button_error');
        };
        if (user_answer == 0) {
            $(".button:eq(0)").addClass('button_yes');
        } else {
            $(".button:eq(1)").addClass('button_no');
        };
        $('.button').addClass('button_disabled')
        etsCommon.showOrHideAnalysis();
    },

    // 设置答案
    kttb_setAllInputAnswer: function(arrStr) {
        if ($('input').length > 0) {
            var arr = eval("(" + arrStr + ")");
            $('.fill_word .number').addClass('white');
            $(arr).each(function(index, item) {
                var id = item.id;
                var answer = item.answer;
                var b = item.isCorrect;
                var parent =  $('input[id="' + id + '"]').parent();
                if(answer.length >= 8){
                    parent.css('width', answer.length * 32 / 75 + 'rem');
                }
                $('input[id="' + id + '"]').val(answer).addClass('white').attr('readonly', true).siblings('.fill_number').addClass('none');
                if (b) {
                    parent.addClass('bg_darkGreen no_border');
                } else {
                    parent.addClass('bg_darkRed no_border');
                };
            });
            etsCommon.showOrHideAnalysis();
        } else {
            setTimeout(function() {
                etsCommon.kttb_setAllInputAnswer(arrStr);
            }, 200);
        }
        
    },

    kttb_getWord: function() {
        var id = $('.write_input').attr('id').trim();
        var word = $('.write_input').val().trim();
        var ua = navigator.userAgent.toLowerCase();
        if (/iphone|ipad|ipod/.test(ua)) {
            location.href = "ets://get_word?qid=" + id + "&word=" + word;
        } else if (/android/.test(ua)) {
            window.webInteraction.getWord(id, word);
        }
    },

    kttb_rmDivBottom: function() {
        $('.kttb_setDivBotton').remove();
    },

    kttb_setDivBotton: function(height) {
        $('.kttb_setDivBotton').remove();
        var div = $('<div>').addClass('kttb_setDivBotton');
        div.css({ 'width': '100%' });
        div.css('height', height + 'px');
        div.css('min-height', height + 'px');
        div.css('margin', '0');
        div.css('padding', '0');
        $('body').append(div);
    },

    kttb_scrollTop: function(height) {
        $(document, "body", ".container").scrollTop(height)
    },

    // 上一道
    kttb_preSubject1: function(posi) {
        var arr = etsCommon.kttb_getAllInput();
        var f_id = $('input:focus').attr('id');
        if (f_id) {
            var index = arr.indexOf(f_id);
            if (index == 0) {
                window.webInteraction.notPre(posi);
            } else {
                $('input:focus').blur();
                $('input').eq(--index).focus();
            };
        }
    },
    // 下一道
    kttb_nextSubject1: function (posi) {
        var arr = etsCommon.kttb_getAllInput();
        var f_id = $('input:focus').attr('id');
        if (f_id) {
            var index = arr.indexOf(f_id);
            if (index == arr.length - 1) {
                window.webInteraction.notNext(posi);
            } else {
                $('input:focus').blur();
                $('input').eq(++index).focus();
            };
        }
    },

    // 获取所有空的id
    kttb_getAllInput: function() {
        var arr = [];
        $('input').each(function(index, item) {
            arr.push($(item).attr('id'));
        })
        return arr;
    },

    showWordTextOrg: function() {
        $(".word_txt").hide();
        $(".word_txt_org").show();
        $(".word_info").show();
        $(".spell_info").show();
    },

    kttb_setChooseAnswer: function(arrStr) {
        var arr = eval("(" + arrStr + ")");
        $('.question1_on').removeClass('question1_on');
        $('.question_on').removeClass('question_on');
        $('.choose1_selected').removeClass('choose1_selected');
        $('.choose2_selected').removeClass('choose2_selected');
        $(arr).each(function(index, item) {
            var number = item.number,
                score = item.score,
                all_score = item.all_score,
                stander_ans = item.stander_ans,
                user_ans = item.user_ans;
            var user_num = etsCommon.atonum(user_ans) - 1;
            var stander_num = etsCommon.atonum(stander_ans) - 1;
            var question = $('#' + number);
            if (question.length == 0) {
                question = $('#choose_' + number);
            };
            if (question.hasClass('question1')) {
                question.find('.choose1_input ').text('');
                //选图
                if (stander_num == user_num) {
                    question.find('.choose1').eq(user_num).addClass('rightB rightPic').siblings().find('.choose1_input').hide();
                    // question.find('.choose1:not(".rightPic")').addClass('errorPic');
                } else {
                    question.find('.choose1').eq(stander_num).addClass('rightB rightPic hasPic');
                    question.find('.choose1').eq(user_num).addClass('errorB errorPic hasPic');
                    question.find('.choose1:not(".hasPic")').find('.choose1_input').hide();
                    // question.find('.choose1:not(".rightPic")').addClass('errorPic');
                };
            } else if (question.hasClass('question2')) {
                //短对话
                if (stander_ans == user_ans) {
                    question.find('.choose2').eq(user_num).removeClass('red green').addClass('green').find('.choose2_input').removeClass('choose2_input_error').addClass('choose2_input_right');
                } else {
                    question.find('.choose2').eq(stander_num).removeClass('red green').addClass('green').find('.choose2_input').removeClass('choose2_input_error').addClass('choose2_input_right');
                    question.find('.choose2').eq(user_num).removeClass('red green').addClass('red').find('.choose2_input').removeClass('choose2_input_right').addClass('choose2_input_error');
                };
            }
            if (user_ans !== 'Z') {
                question.addClass('question_has_ans');
            };
        })
        $('.question1:not(".question_has_ans"),.question2:not(".question_has_ans")').addClass('question_no_ans');
        $(document).off('click', '.choose1');
        $(document).off('click', '.choose2');
        etsCommon.showOrHideAnalysis();

        //显示原文录音
        $(".original-text").show();
    },

    kttb_checkWriteAns: function() {
        $('.write_tip').hide();
        $('.analysis').fadeIn();
    },

    showNote: function(){
        $(".word_con_1").hide()
        $(".word_con_2").show();
    },

    hidenNote: function(){
        $(".word_con_2").hide()
        $(".word_con_1").show();
    },

    // 显示短文原文
    showOriginal: function(){
        $(".original_con").show().siblings().hide();
    },

    // 显示短文翻译
    showTranslate: function(isShow){
        if ($.trim($(".translate_con").text()).length > 0) {
            $(".translate_con").show().siblings().hide();
        } else {
            if (isShow) {
                etsCommon.showOriginal_w_symbol();
            }
        }
    },

    showOriginal_w_symbol: function(){
        $(".original_w_symbol_con").show().siblings().hide();
    },

    setWordText: function(word_txt){
        $(".word_txt").html(word_txt);
        $(".word_info").show();
    },
    setWordMask: function(text) {
        $(".words_mark").html(text);
    },

    showWordError: function(){
        var prama_len = arguments.length;
        $(".word_eval_result_con .in_b").empty();
        for (var i = 0; i < prama_len; i++) {
            var p = $("<p>");
            var text = arguments[i];
            p.html(text);
            $(".word_eval_result_con .in_b").append(p);
        };
        $(".word_eval_result_con").show();
    },

    setEvent: function(){
        $(".word").click(function() {
            var word = $(this).text();
            var x = $(this).offset().top;
            var y = $(this).offset().left;
            window.webInteract.showPopWindow(word, x, y);
        });
    },

    getSentenceText1: function(isHtml){
        var content = $(".word_con_1 .word_txt");
        if (content.length < 1) {
            content = $(".word_con_1 .se_txt");
        }
        if (content.length > 0) {
            if (isHtml) {
                var text = content.html();
                etsCommon.getHtml1Content(text);
            } else {
                var text = content.text().trim();
                etsCommon.getHtml1Content(text);
            }
        }
    },

    getSentenceText2: function(){
        var content = $(".word_con_2 .word_txt");
        if (content.length < 1) {
            content = $(".word_con_1 .se_txt");
        }
        if (content.length > 0) {
            var text;
            content.each(function getText(i) {
                if (i == 0) {
                    text = $(this).text().trim();
                    etsCommon.getHtml2Content(text);
                }
            })
        }
    },

    setSentenceText1: function(srcText){
        var srcContent = $(".word_con_1 .word_txt");
        if (srcContent.length < 1) {
            srcContent = $(".word_con_1 .se_txt");
        }
        if (srcContent.length > 0) {
            srcContent.each(function setText(i) {
                if (i == 0) {
                    $(this).html(srcText);
                }
            })
        }
    },

    setSentenceText2: function(srcText, isShow){
        var srcContent = $(".word_con_2 .word_txt");
        if (srcContent.length < 1) {
            srcContent = $(".word_con_2 .se_txt");
        }
        if (srcContent.length > 0) {
            srcContent.each(function setText(i) {
                if (i == 0) {
                    $(this).html(srcText);
                }
            })
        }
    },

    getHtml1Content: function(text) {
       try{
        window.webInteract.setHtmlText1(text);
       }catch(err) {
        console.log(err)
       }
    },
    
    getHtml2Content: function(text) {
        try{
            window.webInteract.setHtmlText2(text);
        }catch(err) {
            console.log(err)
        }
    },

    // 引藏导读
    hideGuideReading: function(){
        $(".guide_reading").hide();
    },

    // 播放原文录音
    stander_audio: function(id, path) {
        if (this.system == 1) {
            location.href = "ets://stander_audio?path=" + path + "&recordId=" + id;
        } else if (this.system == 2) {
            if(path!= ''){
                try {
                    window.webInteraction.playOrgAudio(id, path);
                } catch (e) {}
            }else {
                try {
                    window.webInteraction.playOrgAudio(id);
                } catch (e) {}
            }
        }
    },
    /**播放用户录音**/
    /**
     ***        id: 题号
     ***        fileId: 录音名
     ***        path: 
     **/
    user_audio: function(id, fileId, path) {
        if (this.system == 1) {
            location.href = "ets://user_audio?qid=" + id + "&recordId=" + fileId;
        } else if (this.system == 2) {
            window.webInteraction.playRecordAudio(id, fileId);
        }
    },
    /**隐藏choose xt_xh**/
    /**隐藏fill xth**/
    /**
     ***        id: 题号
     ***        fileId: 录音名
     ***        path: 
     **/
     hideXt: function(str){
        if (typeof str === 'string') {
            str = JSON.parse(str)
        }
        str.forEach(function(item,index){
            $("p[data-xth="+item+"], div[data-xth="+item+"], div[data-xt_xh="+item+"]").hide();
        })
     },
    //  音素级别报告
    setPhonemeInfo: function(data){
        var that = this;
        var phonemeData = data || {};
        templateApp.phonemeInfo = phonemeData;
        // 判断是否需要展示展示更多的按钮、延时是为了处理 ios单词未渲染，导致show-more按钮未显示
        setTimeout(function(){
            var scrollContents = $('.words-scroll-content');
            scrollContents.each(function(index,item){
                var dom = $(item);
                var domHeight= dom.height();
                var wordsWrapper = dom.find('.words-wrapper');
                var wordsWrapperHeight = wordsWrapper.height();
                var word = wordsWrapper.find('.list-item');
                var marginBottom = parseInt(word.css('margin-bottom')) || 0;
                if(wordsWrapperHeight - marginBottom > domHeight + 5) {
                    dom.siblings('.title-wrapper').find('.show-more').removeClass('none')
                }
            })
        }, 1000)
    },
    // 单词释义弹窗
    showWordPopup: function(data) {
        var special_word = [ 'approx.', 'A.D.', 'A.M.', 'a.m.', 'B.C.', 'Dr.', 'etc.', 'Mr.', 'Mrs.', 'Ms.', 'P.E.', 'P.M.', 'p.m.', 'sec.', 'U.S.']
        var word = data.word;
        var x = data.x;
        var y = data.y;
        var width = data.width;
        var height = data.height;
        // if(special_word.indexOf(word) === -1) {
        //     var start = 0, end = word.length;
        //     for(var i = 0; i < word.length; i++) {
        //         start = i;
        //         if(word[i].match(/[a-zA-Z]/)) break
        //     }
        //     for(var i = word.length - 1; i > 0; i--) {
        //         if(word[i].match(/[a-zA-Z]/)) break
        //         end = i
        //     }
        //     word = word.substring(start, end)
        // }
        console.log(word)
        try{
            if(this.system === 1) {
                window.location.href = 'ets://dimensionality_word?word='+word+'&x='+x+'&y='+y+'&width='+width+'&height='+height
            }else if(this.system === 2) {
                window.webInteraction.showWordSearchDialog(height, x, y, word)
            }
        }catch(err) {
            console.error(err)
        }
    },
    // 音标弹窗
    showPhonemePopup: function(data) {
        var word = data.word;
        var x = data.x;
        var y = data.y;
        var width = data.width;
        var height = data.height;
        try{
            if(this.system === 1) {
                window.location.href = 'ets://dimensionality_phonetic?word='+word+'&x='+x+'&y='+y+'&width='+width+'&height='+height
            }else if(this.system === 2) {
                window.webInteraction.showPhoneticDialog(word)
            }
        }catch(err) {
            console.error(err)
        }
    },
    // 失分单词，失分音素提示语弹窗
    showPopMessage: function(data) {
        var type = data.type;
        try{
            if(this.system === 1) {
                window.location.href = 'ets://dimensionality_popMessage?type=' + type
            }else if(this.system === 2) {
                window.webInteraction.showTipsDialog(type)
            }
        }catch(err) {
            console.error(err)
        }
    },
    // 单词，音标等弹窗消失的回调
    popDismiss: function() {
        // 取消高亮状态
		$('.pop-word.active, .word-analysis .word-item.active, .word-analysis .phoneme-item.active').removeClass('active');
    },
    // 课堂听读播放录音
    playWordPronunciationAudio: function(src) {
        if(templateApp && templateApp.playWordPronunciationAudio){
            templateApp.playWordPronunciationAudio(src)
        }
    },
    getOssUrl: function(url) {
        try{
            if(this.system === 1) {
                window.location.href =  'ets://getOssUrl?url='+url  
            }else if(this.system === 2) {
                window.webInteraction.playWordPronunciationAudio(url)
            }
        }catch(err) {
            console.error(err)
        }
    },
    setWordPronunciationAudioIconStatus: function(playing){
        if(templateApp && templateApp.setIconStatus){
            templateApp.setIconStatus(playing)
        }
    },
    // 安卓暂停播放音频
    stopWordPronunciationAudio: function() {
        if(this.system === 2) {
            window.webInteraction.stopWordPronunciationAudio(url)
        }
    },
    // 复述题设置诊断进度
    setRepeatQuestionDiagnosisProgress: function(progress) {
        if(templateApp && templateApp.setRepeatQuestionDiagnosisProgress){
            templateApp.setRepeatQuestionDiagnosisProgress(progress)
        }
    },
    // 设置诊断按钮 状态  
    // 0： 显示开始诊断
    // 1： 显示诊断进度
    // 2： 显示诊断完成
    setRepeatDiagnosisStatus: function(status) {
        if(templateApp && templateApp.setRepeatDiagnosisStatus){
            templateApp.setRepeatDiagnosisStatus(status)
        }
    },
    // 诊断完成，设置诊断数据
    setRepeatQuestionDiagnosisData: function(data, keypoint) {
        var list = []
        var keypointList = []
        if(templateApp && templateApp.setRepeatQuestionDiagnosisData){
            try{
                list = JSON.parse(data)
                keypointList = JSON.parse(keypoint)
            }catch(err) {
                try {
                    list = new Function('return ' + data)();
                    keypointList = new Function('return ' + keypoint)();
            }catch(err){

                }
            }
            if(Array.isArray(list)) {
                list.forEach(function(item) {
                    for(var i = 0; i < keypointList.length; i++) {
                        if(parseInt(item.keypoint) === parseInt(keypointList[i].index)) {
                            item.keypointValue = keypointList[i].content
                            break
                        }
                    }
                })
                templateApp.setRepeatQuestionDiagnosisData(list.map(function(item) {
                    if(typeof item === 'string') {
                        try{
                            item = JSON.parse(item)
                        }catch(err){}
                    }
                    return  item
                }))
            }else {
                templateApp.setRepeatQuestionDiagnosisData([])
            }
        }
    },
    getDiagnosisDataFromList: function(list) {
        var list = list || []
        var res = ''
        list.length && list.forEach(function(item){
            item.choices && item.choices.text && item.choices.text.length && item.choices.text.forEach(function(textItem){
                res += textItem.content || ''
            })
        })
        return eval(res)
    },
    // 点击开始诊断，调用客户端方法
    startDiagnosis: function() {
        try{
            if(this.system === 1) {
                window.location.href =  'ets://start_diagnosis'  
            }else if(this.system === 2) {
                window.webInteraction.startDiagnosis()
            }
        }catch(err) {
            console.error(err)
        }
    },
    stopDiagnosis: function() {
        try{
            if(this.system === 1) {
                window.location.href =  'ets://stop_diagnosis' 
            }else if(this.system === 2) {
                window.webInteraction.stopDiagnosis()
            }
        }catch(err) {
            console.error(err)
        }
    },

    setFontSizeBySelector: function(selector, size){
        $(selector).css('font-size', size / 75 + 'rem')
    }
}

// ========== 二期新增：SSE 流式推送接收层 ==========
// Native 通过 evaluateJavascript 调用以下 window 全局函数
// 挂在 window 上（非 webInteraction/webInteract），三端统一

/**
 * Native 每次收到 SSE 数据包时调用
 * @param {string|Object} data - DiagnosisEventData JSON 字符串或对象
 */
window.onNativeDiagnosisEvent = function(data) {
    try {
        var eventData = typeof data === 'string' ? JSON.parse(data) : data;
        if (window.templateApp && window.templateApp.handleDiagnosisEvent) {
            window.templateApp.handleDiagnosisEvent(eventData);
        }
    } catch(err) {
        console.error('[onNativeDiagnosisEvent] parse error:', err);
    }
};

/**
 * SSE 连接正常关闭时调用
 * @param {Object} [options] - 可选参数，{isPartial: true} 表示超时导致的不完整结果
 */
window.onNativeDiagnosisDone = function(options) {
    try {
        var opts = typeof options === 'string' ? JSON.parse(options) : (options || {});
        if (window.templateApp && window.templateApp.handleDiagnosisDone) {
            window.templateApp.handleDiagnosisDone(opts);
        }
    } catch(err) {
        console.error('[onNativeDiagnosisDone] error:', err);
    }
};

/**
 * SSE 异常/超时/网络断开时调用
 * @param {string|Object} errorInfo - ErrorInfo JSON 字符串或对象
 */
window.onNativeDiagnosisError = function(errorInfo) {
    try {
        var info = typeof errorInfo === 'string' ? JSON.parse(errorInfo) : (errorInfo || {});
        if (window.templateApp && window.templateApp.handleDiagnosisError) {
            window.templateApp.handleDiagnosisError(info);
        }
    } catch(err) {
        console.error('[onNativeDiagnosisError] parse error:', err);
    }
};

// 失去焦点 
$(document).on('blur', 'input, textarea', function(e) {
    if (window &&  window.webInteraction) {
        try{
            window.webInteraction.kttp_blurFocus();
        }catch(err) {

        }
    } 
})