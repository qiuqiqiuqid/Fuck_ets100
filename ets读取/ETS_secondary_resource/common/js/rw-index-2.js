//  question type    
//  1 => '根据首字母题型'
//  2 => '根据中文提示/适当的形式填空'
//  3 => '完成句子'
//  4 => '单项选择'
//  5 => '短文填空'
//  6 => '完型填空'
//  7 => '阅读理解'
//  8 => '阅读填空'
//  9 => '阅读回答'
//  10 => '任务型阅读'
//  11 => '首字母短文填空'

// info type
//1-选择题 2-阅读填空题 3-短文填空题 4-回答题 5-中译英 6-英译中
//7-完成句子 8-完形填空 9-首字母填空 10-首字母短文填空

$(function() {
     var blankReg = {
        szm: /{{blank\/([A-Za-z]*)\/}}/g,
        normal: /{{blank}}/g,
        normal_mult: /{{blankmult}}/g,
        num_mult: /{{blankmult([0-9]+)}}/g,
        sel_single: /{{blanksel}}/g,
        sel_mult: /{{blanksel([0-9]+)}}/g,
        line_mult: /{{line([0-9]+)}}/g,
    }

    // 正则替换 {{blank}}等规则
    var nonum_input = '<span class="fill_word fill_word_nonum"><input type="text" class="fill_word_input"></span>';
    var szm_input = '<span class="fill_word fill_word_nonum "><input type="text" class="fill_word_input" data-szm="{0}"></span>';
    var num_input = '<span class="fill_word fill_word_sel"><span class="number">{0}</span><span class="fill_number">{0}</span><input type="text" class="fill_word_input"></span>';
    var sel_input = '<span class="fill_word fill_word_sel"><span class="number">{0}</span><span class="fill_number">{0}</span><input type="text" class="fill_word_input" readonly="true"></span>';
    var sel_single_input = '<span class="fill_word fill_word_sel"><span class="number"></span><span class="fill_number"></span><input type="text" class="fill_word_input" readonly="true"></span>';
    var line_input = '<span class="fill_sentence fill_word fill_word_sel"><span class="number">{0}</span><span class="fill_number">{0}</span><input type="text" class="fill_word_input"></span>';

    // 任务型阅读 正则替换 {{blank}}等规则
    var rwx_nonum_input = '<span class="fill_word fill_word_nonum"><span class="number"></span><span class="fill_number"></span><input type="text" class="fill_word_input" data-order="{0}"></span>';
    var rwx_szm_input = '<span class="fill_word fill_word_nonum "><span class="number"></span><span class="fill_number"></span><input type="text" class="fill_word_input" data-szm="{0}" data-order="{1}"></span>';
    var rwx_num_input = '<span class="fill_word fill_word_sel"><span class="number">{0}</span><span class="fill_number">{0}</span><input type="text" class="fill_word_input" data-order="{1}"></span>';
    var rwx_sel_input = '<span class="fill_word fill_word_sel"><span class="number">{0}</span><span class="fill_number">{0}</span><input type="text" class="fill_word_input" readonly="true" data-order="{1}"></span>';
    var rwx_sel_single_input = '<span class="fill_word fill_word_nonum fill_word_sel"><span class="number"></span><span class="fill_number"></span><input type="text" class="fill_word_input" readonly="true" data-order="{0}"></span>';
    var rwx_line_input = '<span class="fill_sentence fill_word fill_word_sel"><span class="number">{0}</span><span class="fill_number">{0}</span><input type="text" class="fill_word_input" data-order="{1}"></span>';

  

    function replaceBlankReg(val, order, infoIndex) {
        var str = '';
        var index = dxApp.questionIndex - 1;
        // var val = "He is good at {{blank/waASFaa/}} beautiful {{blank}}  {{blank}} passages {{blankmult}} {{blankmult1}} {{blankmult12}} {{blanksel1}} {{blanksel13}}.";
        if (typeof val === 'string') {
            val = val.replace(/&quot;Times New Roman&quot;/gi, '').replace(/&quot;serif&quot;/g, '')
            val = _.unescape(val); // 处理转义字符
            if (webviewSystem === 1 && screen.height <= 570) {
                val = val.replace(/\<span[^\>]+\>/gi, function(match, p1, p2) {
                    return ''
                }).replace(/<\/span>/gi, function(match, p1, p2) {
                    return ''
                })
            }
            if(dxApp.question.type == 10){
                order = Number(order);
                if(typeof infoIndex != "undefined"){
                    infoIndex = Number(infoIndex);
                }else{
                    infoIndex = 0;
                }
                var infoItem = dxApp.question.info[infoIndex];
                if(infoItem.sub_question_info.length===1&&infoItem.type!=9&&infoItem.type!=10){
                    order = order + 1;
                }
                // 解析   任务型阅读  order需要转化为sub_question_info中的order
                str = val.replace(blankReg.szm, function(match, p1, offset, str) {
                        // p1 为匹配的字母 
                        return p1 + rwx_szm_input.format(p1, order)
                    }).replace(blankReg.normal, function(match, offset, str) {
                        return rwx_nonum_input.format(order)
                    }).replace(blankReg.normal_mult, function(match, offset, str) {
                        return rwx_nonum_input.format(order)
                    }).replace(blankReg.num_mult, function(match, p1, offset, str) {
                        // p1 为匹配的数字
                        p1 = Number(p1);
                        var p = p1 + index + order - infoIndex
                        return rwx_num_input.format(p, order + p1)
                    }).replace(blankReg.sel_mult, function(match, p1, offset, str) {
                        // p1 为匹配的数字
                        p1 = Number(p1);
                        var p = Number(p1) + index + order - infoIndex
                        return rwx_sel_input.format(p, order + p1)
                    }).replace(blankReg.sel_single, function(match, p1, offset, str) {
                        return rwx_sel_single_input.format(order)
                    }).replace(blankReg.line_mult, function(match, p1, offset, str) {
                        // p1 为匹配的数字
                        p1 = Number(p1);
                        var p = p1 + index + order - infoIndex
                        return rwx_line_input.format(p, order + p1)
                    })
            }else{
                str = val.replace(blankReg.szm, function(match, p1, offset, str) {
                        return p1 + szm_input.format(p1)
                    }).replace(blankReg.normal, function(match, offset, str) {
                        return nonum_input
                    }).replace(blankReg.normal_mult, function(match, offset, str) {
                        return nonum_input
                    }).replace(blankReg.num_mult, function(match, p1, offset, str) {
                        return num_input.num_format(p1, index)
                    }).replace(blankReg.sel_mult, function(match, p1, offset, str) {
                        return sel_input.num_format(p1, index)
                    }).replace(blankReg.sel_single, function(match, offset, str) {
                        return sel_single_input
                    }).replace(blankReg.line_mult, function(match, p1, offset, str) {
                        return line_input.num_format(p1, index)
                    })
            }
            str = replaceBlankRegCM(str)
        }
        return str
    }
    // var webviewSystem=1;
    var dxApp = new Vue({
        el: "#dxApp",
        data: {
            deepReloaded: false,
            system: webviewSystem,
            question: {},
            info: {},
            answerPage: false,
            userAns: '',
            standerAns: '',
            canSetAnswer: false,
            questionIndex: 1,
            ansStr: [],
            noShowAll: [],
            // typeNineAnalysis: false,
            // typeTenAnalysis: false
        },
        mounted: function() {
            $("div.container").removeClass("hidden");
            setTimeout(function() {
                if ($('.input-answer').length > 0) {
                    if ($('.input-answer')[0].tagName == 'TEXTAREA') {
                        // $('.input-answer').flexText();
                    }
                    // $('.flex-text-wrap').each(function(index,item){
                    //     $(item).height($(item).children('.input-answer').prop('scrollHeight'))
                    // })

                }

                // $('img').width()
            }, 10)
        },
        updated: function() {
            var that = this;
            this.canSetAnswer = true;
            if ($('.input-answer').length > 0) {
                if ($('.input-answer')[0].tagName == 'TEXTAREA') {
                    $('.input-answer').flexText();
                }
            }
            // 图片自适应手机
            $('img:not(.not-change-style)').each(function(index, item) {
                var div = $('<div class="show-big-img-tip">查看大图</div>');
                $(item).width('100%');
                $(item).parent().addClass('show-big-img').append(div);
            })
            // 题目序号
            $('.fill_word').addClass('fill_word_sel');
            if(this.question.type == 10){
                var number = that.questionIndex;
                var sub_num = 0; // 一个sub_question_info，对应多个空的时候，会对后面的空格照成影响，需要减去input数量和sub_question_info的length之差
                $('.fill_word_nonum,.import').each(function(index, item){
                    var fillblank = $(item).parents('.fillblank');
                    var que_index = fillblank.attr("data-index"); // info的index值
                    var fillblank_item = fillblank.find('.fill_word_nonum,.import');
                    var fillblank_item_index = fillblank_item.index($(item));  // 当前input或textarea 属于该题的第几个空
                    var input_index = $('.fill_word,.import').index($(item));  //该input 或textarea属于该页面第几个空
                    
                    // console.log('index----------------:', index)
                    // console.log('questionIndex:',number)
                    // console.log('fillblank_item_index:',fillblank_item_index)
                    // console.log('sub_num:',sub_num)
                    // console.log('input_index:',input_index)
                    $(item).children('.number,.fill_number').text(number + input_index - sub_num);
                    if(fillblank_item.length > that.question.info[que_index].sub_question_info.length 
                        && fillblank_item_index != fillblank_item.length - 1){
                        // fillblank_item_index = fillblank_item.index($(item)); 
                        // 如果这道题有多个空，而只有一个sub_question，则后面碰到相同的，需要减去（多个空的长度 - 1）的累计数量 
                        // console.log('que_index:::::', que_index)
                        sub_num += 1;//fillblank_item_index;
                        // input_index += fillblank_item_index; //当前info不需要减去当前的  length之差
                    }
                    // $(item).children('.number,.fill_number').text(input_index - fillblank_item_index - sub_num + that.questionIndex);
                })
            }
            this.deepReloaded = true;

            $('.fill_word').each(function(index, item){
                var w = $(item).find('.number').width() || 0;
                if(w){
                    $(item).find('.fill_word_input').css('padding-left', w+10)
                }
            })
        },
        computed: {
            showInfoContent: function() {
                var type = this.question.type;
                var bool = false;
                if (type) {
                    switch (type) {
                        case 1:
                            bool = true;
                            break
                        case 2:
                            bool = true;
                            break
                        case 3:
                            bool = true;
                            break
                        case 4:
                            bool = true;
                            break
                        case 5:
                            // bool = true;
                            break
                        case 6:
                            break
                        case 7:
                            break
                        case 9:
                            // bool=true
                            break
                        case 13:
                            bool = true;
                            break
                        default:
                            break
                    }
                }
                return bool
            }
        },
        methods: {
            getInfoTag: function(tag) {
                var val = '';
                $(tag).each(function(index, item) {
                    val += item + '；'
                })
                return val.substring(0, val.length - 1) + '。';
            },
            formatQuestionContent: function(val) {
                var str = '';
                var index = this.questionIndex;
                if (val) {
                    val = '<div>' + val + '</div>'
                    var div = $("<div>");
                    var html = $(val);
                    div.append(html)
                    div.removeAttr('color face').find("*").removeAttr('color face');
                    str = div[0].outerHTML;
                    str = replaceBlankReg(str, index);
                }
                return str
            },
            formatInfoContent: function(val, order, infoIndex) {
                var str = '';
                if(this.question.type == 10){
                    str = replaceBlankReg(val, order, infoIndex);
                }else{
                    str = replaceBlankReg(val, order);
                }
                return str
            },
            // 解析type为10的题型
            formatTenInfoContent: function(val,subInfoIdx) {

                var index = this.questionIndex+Number(val.order)-subInfoIdx;
                var str = '';
                str = replaceBlankReg(val.content, index);
                return str
            },
            // 是否是用textarea框
            isImport: function(val,order,infoIndex) {
                var index = this.questionIndex;
                var str = '';
                if(this.question.type == 10){
                    str = replaceBlankReg(val, order, infoIndex);
                }else{
                    str = replaceBlankReg(val, order);
                }
                val = replaceBlankRegCM(val);
                
                if (str == val) {
                    return true
                } else {
                    return false
                }
            },
            bandoneon: function(e) {
                e.preventDefault();
                if (e.target.nodeName == 'DIV') {
                    var index = $(e.target).data('index');
                    this.$set(this.noShowAll, index, !this.noShowAll[index])
                } else {
                    var index = $(e.target).parent().data('index');
                    this.$set(this.noShowAll, index, !this.noShowAll[index])
                }
            },
            slashToArr: function(str) {
                return str.replace(/<br \/>/g, '').split('/')
            },
            formatToHtml: function(str){
                return str.replace(/\r\n|\r|\n|↵|\\r\\n|\\r|\\n/g, '<br />')
            }
        }
    })
    window.dxApp = dxApp;
    // setDxQuestion(data.body.combination[0]['question'][0],data.body.combination[0]['question'][0]['info'],1)
    // setDxQuestion(D10.data[2]['question'][0],D10.data[2]['question'][0]['info'],1)
    // setDxQuestion(D4.data['question'][0],D4.data['question'][0]['info'][0],10)
     // 填空题正则
   
    //******** input 效果*********//
    // ios点击空白处失去焦点
    document.querySelector('body').addEventListener('touchend', function(e) {
        var len = document.querySelectorAll('.fill_word_input').length;
        for (var i = 0; i < len; i++) {
            document.querySelectorAll('.fill_word_input')[i].blur()
        }
    });
    // 输入框获取焦点时，若输入框值为空，则将父元素背景设为白色
    $(document).on("focus", ".fill_word_input", function(e) {
        $('.fill_word_noBgC').removeClass('fill_word_noBgC');
        $(this).parent().removeClass('fill_word_lightblue').addClass('fill_word_noBgC');
        var input = $(this);
        var info_index = $('.fill_word_input').index(input);
        var document_height = $(document).height(),
            scroll_height = $('.container').scrollTop(),
            input_height = input.height(),
            input_offset_top = input.offset().top;
        var dpi = window.devicePixelRatio;
        var order;
        // 任务型阅读 读取order
        if (dxApp.question.type == 10) {
            order = $(this).attr("data-order");
            order = Number(order);
            // 如果是首字母填空，这需要加上当前首字母的index
            if($(this).attr("data-szm") != undefined){
                // 当前input 父级元素下找到首字母的input
                var now_szm_input = $(this).parents('.fillblank').find('.fill_word_input[data-szm]');
                var index = now_szm_input.index($(this))+1;
                order = order + index;
            }
        }else{
            if (dxApp.question.info.length > 1) {
                order = dxApp.question.info[info_index]['order']
            } else {
                order = dxApp.info.order;
            }
        }
        console.log('order',order)
        // isFill
        var isFill = 0;
        if(input.val().trim()&&input.attr('data-error')==1){
            isFill = 1;
        }
        if (webviewSystem === 2) {
            window.webInteraction.kttb_blankFocus(order, input_offset_top, input_height, scroll_height, document_height, dpi);
        } else if (webviewSystem === 1) {
            location.href = "ets://blank_get_focus?isFill=" + isFill + "&order=" + order + "&inputOffset=" + input_offset_top + "&inputHeight=" + input_height + "&scrollOffset=" + scroll_height + "&dpr=" + dpi;
        }
    })
    // 输入框失去焦点时，若输入框值为空，则将父元素背景设为蓝色
    $(document).on("blur", ".fill_word_input", function(e) {
        var val = $(this).val().trim();
        $(this).parent().removeClass('fill_word_noBgC fill_word_lightblue');
        if (val !== '') {
            $(this).parent().addClass('fill_word_lightblue');
        }
        if (webviewSystem === 2) {
            window.webInteraction.kttp_blurFocus();
        } else if (webviewSystem === 1) {
            // location.href = "ets://blank_blur_focus?blur=true";
        }
    })
    // 完形填空选择题
    $(document).on("click", ".fill_word.fill_word_sel", function(e) {
        e.stopPropagation();
        if($(this).find('input[readonly]').length == 0){ return }
        var info_index = $('.fill_word.fill_word_sel').index($(this)) //当前空格的 index
        // 调用弹出某个子面板
        // showZMB(index);
        // 结束
        if (!$(this).find('input').eq(0).hasClass('answer-disabled-input')) {
            $('.fill_word.fill_word_sel').each(function(index, item) {
                var val = $(item).val();
                if (val) {
                    $(item).removeClass('fill_word_noBgC').addClass('fill_word_lightblue');
                } else {
                    $(item).removeClass('fill_word_noBgC fill_word_lightblue');
                }
            })
            $("input.fill_word_input").each(function(index, item) {
                if ($(item).val() != '') {
                    $(item).parent().addClass('fill_word_lightblue');
                }
            })
            $(this).removeClass('fill_word_lightblue').addClass('fill_word_noBgC');
        }
        var input = $(this);
        var document_height = $(document).height(),
            scroll_height = $('.container').scrollTop(),
            input_height = input.height(),
            input_offset_top = input.offset().top;
        var dpi = window.devicePixelRatio;
        // 填空题如果字符超过14个，点击显示toast，显示全部内容
        $('.toast-div').remove()
        var inputVal = $(this).find('input').eq(0).val()
        if(inputVal.length > 0) {
            var height = input.height()
            var windowHeight = $(window).height()
            var windowWidth = $(window).width()
            var scrollTop = $(document).scrollTop()
            var offset = input.offset()
            var offsetTop = offset.top
            var top = offsetTop - scrollTop // 距离屏幕顶部距离
            var toastDiv = $('<div class="toast-div">'+inputVal+'</div>')
            var cssOptions = {
            }
            if(top < windowHeight/2) {
                cssOptions.top = height + 10 + 'px'
                toastDiv.addClass('top-arrow')
            } else {
                cssOptions.top = (-height - 10) + 'px'
                toastDiv.addClass('bottom-arrow')
            }
            $(this).append(toastDiv)
            toastDiv.css(cssOptions)
            if($('.toast-div').offset().left < 0) {
                $('.toast-div').css({
                    right: 'auto',
                    left: '0',
                    transform: 'none'
                }).addClass('arrow-in-left')
            } else if($('.toast-div').offset().left + $('.toast-div').width() > windowWidth) {
                $('.toast-div').css({
                    left: 'auto',
                    right: '0',
                    transform: 'none'
                }).addClass('arrow-in-right')
            }
        }
        var order = 0
        // 任务型阅读 读取order
        if (dxApp.question.type == 10) {
            order = $(this).find('input').attr("data-order");
            order = Number(order);
            // 如果是首字母填空，这需要加上当前首字母的index
            if($(this).find("input").attr("data-szm") != undefined){
                // 当前input 父级元素下找到首字母的input
                var now_szm_input = $(this).parents('.fillblank').find('.fill_word');
                var index = now_szm_input.index($(this))+1;
                order = order + index;
            }
        }else{
            if (dxApp.question.info.length > 1) {
                order = dxApp.question.info[info_index]['order'];
            } else {
                order = dxApp.info.order;
            }
        }
        console.log('order', order)

        // isFill
        var isFill = 0;
        if($(this).find("input").val().trim()&&$(this).find("input").attr('data-error')==1){
            isFill = 1;
        }
        if (webviewSystem === 2) {
            window.webInteraction.kttb_blankFocus(order, input_offset_top, input_height, scroll_height, document_height, dpi);
        } else if (webviewSystem === 1) {
            location.href = "ets://blank_get_focus?isFill=" + isFill + "&order=" + order + "&inputOffset=" + input_offset_top + "&inputHeight=" + input_height + "&scrollOffset=" + scroll_height + "&dpr=" + dpi;
        }
    })
    // // 输入框输入内容时变换宽度
    $(document).on('input', ".fill_word_input", function(e) {
        setInputWidht($(this))
        // if(!$(this).parent().hasClass('fill_sentence')){
        //     var val = $(this).val().trim();
        //     if (val.length > 0 && val.length < 11) {
        //         $(this).parent().css('width', '2.985075rem');
        //     } else if (val.length >= 11) {
        //         $(this).parent().css('width', '3.880597rem');
        //     };
        // }
    })
    // 输入框输入内容时变换宽度 fill_sentence
    // $(document).on('input', ".fill_sentence .fill_word_input", function(e) {
    //     var val = $(this).val();
    //     var len = val.length;
    //     var fontSize = parseFloat($(this).css('font-size')) || 16*window.devicePixelRatio;
    //     var width = $(this).width();
    //     var margin = parseFloat($(this).parent().css('margin-left')) || 0;
    //     var max_width = $(this).parents('p').width() - 2*margin;
    //     if(len*fontSize > width){
    //         if(len*fontSize > max_width){
    //             $(this).parent().css('width', max_width);
    //         }else{
    //             $(this).parent().css('width', len*fontSize);
    //         }
    //     }
    // })

    //******** input效果结束 *********//

    //***** 选择题效果 *****//
    // 选项事件
    $(document).on("touchstart", ".single-choose .choose-item", function(e) {
        e.stopPropagation();
        // $(this).removeClass("box-shadow");
    })
    $(document).on("touchmove", ".single-choose .choose-item", function(e) {
        e.stopPropagation();
        // $(this).addClass("box-shadow");
    })
    $(document).on("touchend", ".single-choose .choose-item", function(e) {
        e.stopPropagation();
        // $(this).addClass("box-shadow");
    })
    $(document).on("click", ".choose-con:not(.disabled) .choose-item", function(e) {
        e.stopPropagation();
        $(this).siblings().removeClass('active');
        $(this).addClass('active');
        getRwAnswerStr();
    })
    //***** 选择题效果结束 *****//

    // ******清除初始值*******//
    // 阅读回答题型
    $(document).on('focus', '.input-answer', function(e) {
        var input = $(this);
        var document_height = $(document).height(),
            scroll_height = $('.container').scrollTop(),
            input_height = input.height(),
            input_offset_top = input.offset().top;
        var dpi = window.devicePixelRatio;
        var order;
        // 任务型阅读 读取order
        order = $(this).attr("data-order");
        order = Number(order);
        // if (dxApp.question.type == 10) {
        //     order = $(this).attr("data-order");
        //     order = Number(order);
        // }else{
        //     if (dxApp.question.info.length > 1) {
        //         order = $(this).parents('.fillblank').find(".number").text().trim() - dxApp.questionIndex;
        //     } else {
        //         order = dxApp.info.order;
        //     }
        // }
        console.log('order', order)
        // isFill
        var isFill = 0;
        if(input.val().trim()&&input.attr('data-error')==1){
            isFill = 1;
        }
        if (webviewSystem === 2) {
            window.webInteraction.kttb_blankFocus(order, input_offset_top, input_height, scroll_height, document_height, dpi);
        } else if (webviewSystem === 1) {
            location.href = "ets://blank_get_focus?isFill=" + isFill + "&order=" + order + "&inputOffset=" + input_offset_top + "&inputHeight=" + input_height + "&scrollOffset=" + scroll_height + "&dpr=" + dpi;
        }

    })
    $(document).on('blur', '.input-answer', function(e) {
        // if ($(this).val() == '') {
        //     $(this).attr('placeholder', '请输入答案')
        // }
        if (webviewSystem === 2) {
            window.webInteraction.kttp_blurFocus();
        } else if (webviewSystem === 1) {
            // location.href = "ets://blank_blur_focus?blur=true";
        }
    })
    $(document).on('click',function(e) {
        $('.toast-div').remove()
    })
})