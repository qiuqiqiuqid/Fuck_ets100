"use strict";

// "use strict";
// import Promise from 'es6-promise';
// Promise.polyfill();
var webviewUserAgent = navigator.userAgent.toLowerCase();
var webviewSystem = 8;
if (/iphone|ipad|ipod/.test(webviewUserAgent)) {
    /*ios*/
    webviewSystem = 1;
} else if (/android/.test(webviewUserAgent)) {
    /*android*/
    webviewSystem = 2;
}
function strBlank(str) {
    str = str.replace(/\./g, " ").replace(/\。/g, " ")
        .replace(/\,/g, " ").replace(/\，/g, " ")
        .replace(/\:/g, " ").replace(/\：/g, " ")
        .replace(/\;/g, " ").replace(/\；/g, " ")
        .replace(/\'/g, " ").replace(/\‘/g, " ").replace(/\'/g, " ")
        .replace(/\"/g, " ").replace(/\“/g, " ").replace(/\”/g, " ").replace(/\↵/g, " ").replace(/<br \/>/g, ' ').replace(/\s+/g, ' ');
    return str;
}
function replaceBlankRegCM(val = '') {
    var str = '';
    str = _.unescape(val); // 处理转义字符
    if (typeof str == 'string') {
        str = str.replace(/\{\{italic\}\}/g, '<i>').replace(/\{\{\/italic\}\}/g, '</i>')
            .replace(/\{\{underline\}\}/g, '<u>').replace(/\{\{\/underline\}\}/g, '</u>')
            .replace(/\{\{bold\}\}/g, '<b>').replace(/\{\{\/bold\}\}/g, '</b>')
            .replace(/\{\{center\}\}/g, '<center>').replace(/\{\{\/center\}\}/g, '</center>')
            .replace(/<em>/g, '<i>').replace(/<\/em>/g, '</i>')
            .replace(/\r\n|\r|\n|↵|\\r\\n|\\r|\\n/g, '<br/>')
            .replace(/↵/g, '<br>')
            // .replace(/color:/g, 'nocolor')
            .replace(/font:/g, 'nofont')
            .replace(/font-size:/g, '')
            .replace(/font-family:/g, '')
            .replace(/line-height:/g, '')
            .replace(/margin:/g, '')
            .replace(/padding:/g, '')
        // .replace(/&nbsp;/g, ' ')
    }
    return str
}
/************** app调用方法  开始 **************/
/*  设置问题
 **  @param { object } question , 问题内容   
 **  @param { object } info , 选项信息  
 */
function setDxQuestion(question, info, index, ansStr, errorType) {
    dxApp.canSetAnswer = false;
    dxApp.deepReloaded = false;
    if (dxApp) {
        dxApp.answerPage = false;
    }
    $(".choose-item.active").removeClass("active");
    if (typeof question === 'string') {
        question = question.replace(/\r\n|\r|\n|↵|\\r\\n|\\r|\\n/g, '<br />');
        try {
            question = JSON.parse(question);
        } catch (error) {
            question = new Function('return ' + question)();
        }
    }
    if (info) {
        if (typeof info === 'string') {
            info = info.replace(/\r\n|\r|\n|↵|\\r\\n|\\r|\\n/g, '<br />');
            try {
                info = JSON.parse(info);
            } catch (error) {
                info = new Function('return ' + info)();
            }
        }
    }

    dxApp.questionIndex = Number(index);
    dxApp.question = question;
    dxApp.info = info;
    dxApp.typeNineAnalysis = false
    dxApp.typeTenAnalysis = false

    function setAns(str) {
        if (dxApp.canSetAnswer) {
            setRwAnswerStr(str, errorType);
        } else {
            setTimeout(function () {
                setAns(str);
            }, 10);
        }
    }
    if (ansStr) {
        setAns(ansStr);
    }
}

/*  设置答案
 **  @param { string } userAns , 用户答案   A
 **  @param { string } standerAns , 标准答案  B
 */
function setDxAnswer(userAns, standerAns) {
    dxApp.userAns = userAns;
    dxApp.standerAns = standerAns;
    dxApp.answerPage = true;
}
// 对比答案先处理str
function replaceFuHao(str) {
    var str = str || '';
    return str.replace(/。|，|：|；|‘|’|“|”|,|\.|:|;|'|"/g, '');
}
//ansStr用户答案
function setDxAnswer1(question, info, index, ansStr, panel, errorType) {
    dxApp.deepReloaded = false;
    // 是否是错题本进入
    dxApp.errorType = typeof errorType != 'undefined' ? errorType : 0;

    dxApp.canSetAnswer = false;
    if (typeof question === 'string') {
        question = question.replace(/\r\n|\r|\n|↵|\\r\\n|\\r|\\n/g, '<br />');
        // question=strBlank(question)

        question = JSON.parse(question);
    }
    //---用户答案跟question转为对象
    if (question.type == 3) {
        var ansStr = [JSON.parse(ansStr)];
    } else {
        ansStr = ansStr.replace(/\r\n|\r|\n|↵|\\r\\n|\\r|\\n/g, '<br />');
        // console.log('ansStr', typeof ansStr)
        ansStr = JSON.parse(ansStr);
    }
    dxApp.question = question;

    //-----主面板的标准答案
    //---var standerAnswer:标准答案
    var standerAnswers = [];
    if (panel == 'main') {
        //--------主面板------
        info = question.info;
        //----拿到标准答案
        //---standerAnswers格式[[[hello]],[[hello world],[hello world]]]
        // console.log(info.length)
        for (var i = 0; i < info.length; i++) {
            // var infoAnswer=info[i].answer
            info[i].answer = info[i].answer.replace(/\<br \/\>/g, '')
            //答案中有需要不填的答案，需要/区别来分割，如果第一个字符为/就是不用填的意思
            if (info[i].answer[0] === '/') {
                var arr = ['/'];
                var answer = info[i].answer.substr(2);
                if (answer) {
                    answer = arr.concat(answer.split('/'))
                } else {
                    answer = arr
                }

            } else {
                var answer = info[i].answer.split('/');
            }

            var answerArr = [];
            if (question.type == 3) {
                for (var j = 0; j < answer.length; j++) {
                    if (answer[j].indexOf(';') > -1) {
                        answerArr.push(answer[j].split(';'));
                    } else {
                        answerArr.push(answer[j].split(' '));
                    }
                }
            } else {
                for (var j = 0; j < answer.length; j++) {
                    answerArr.push(answer[j])
                }
            }
            standerAnswers.push(answerArr);
        }
        if (question.type == 1 || question.type == 2 || question.type == 3 || question.type == 4 || question.type == 13) {
            info = question.info[0];
            dxApp.answerPage = true;
        } else {
            dxApp.answerPage = false;
        }
        //----单项选择题ABCD
        if (question.type == 4) {
            // dxApp.answerPage = true;
            dxApp.userAns = ansStr[0];
            dxApp.standerAns = info.answer && info.answer.trim ? info.answer.trim() : info.answer;
        }

    } else if (panel == 'sub') {
        //------子面板
        if (typeof info == 'string') {
            // info = info.replace(/\r\n|\r|\n|↵|\\r\\n|\\r|\\n/g, '<br >');    // 对比答案bug
            info = JSON.parse(info);
        }
        //答案中有需要不填的答案，需要/区别来分割，如果第一个字符为/就是不用填的意思
        if (info.answer[0] === '/') {
            var arr = ['/'];
            var answer = info.answer.substr(2);
            if (answer) {
                answer = arr.concat(answer.split('/'))
            } else {
                answer = arr
            }

        } else {
            var answer = info.answer.split('/');
        }
        // var answer = info.answer.split('/');
        var answerArr = [];
        if (question.type == 3) {
            for (var j = 0; j < answer.length; j++) {
                answerArr.push(answer[j].split(' '));
            }
        } else {
            answerArr.push(answer)
        }
        standerAnswers = answerArr;
        //----单项选择题ABCD
        if (question.type == 5 || question.type == 6 || question.type == 7 || question.type == 8 || question.type == 11) {
            dxApp.answerPage = true;
            dxApp.userAns = ansStr[0];
            dxApp.standerAns = info.answer;
        }
    }
    // 以下和设置答案样式有关
    var newStanderAnswer = [];
    //----回答正确，回答错误 or未作答只有一个info的情况
    if (panel == 'sub' || question.type == 4 || question.type == 1 || question.type == 2 || question.type == 3) {
        //---用户答案为空，未作答
        if (ansStr[0] == '' || ansStr.length == 0) {
            dxApp.standerAnswer = 2;
        } else {
            standerAnswers.map(function (item, index) {
                if (question.type !== 3) {
                    // ansStr[0] = ansStr[0].replace(//g, ' ')
                    ansStr[0] = strBlank(ansStr[0])
                }
                // 标准答案和用户答案对比
                var compareArr = item.filter(function (subItem) {
                    // 用户输入的答案需要将中文单引号替换成英文单引号
                    if (question.type == 5 || question.type == 9) {
                        return strBlank(subItem.toString()).trim() == strBlank(ansStr.toString().replace(/’/g, "'")).trim();
                    } else {
                        return strBlank(subItem.toString().trim()) == strBlank(ansStr.toString().replace(/’/g, "'").trim());
                    }
                })
                if (compareArr.length > 0) {
                    dxApp.standerAnswer = 0;
                } else {
                    dxApp.standerAnswer = 1;
                }
                if (question.type == 3 && compareArr.length == 0) {
                    for (var i = 0; i < item.length; i++) {
                        var subItem = item[i];
                        for (var j = 0; j < subItem.length; j++) {
                            if (subItem[j] == (ansStr[0][j] || '').replace(/’/g, "'")) {
                                dxApp.standerAnswer = 3;
                                break
                            }
                        }
                    }
                }
                // 部分正确
                if (question.type == 5 && compareArr.length == 0) {
                    if (info.keyword) {
                        var keyAns = info.keyword.split('/');
                        keyAns.forEach(function (k_item, keyIdx) {
                            keyAns[keyIdx] = k_item.split(';');
                            var c = keyAns[keyIdx].filter(function (c_item, c_index) {
                                if (ansStr[0]) {
                                    return ansStr[0].toString().indexOf(c_item) > -1;
                                } else {
                                    return false
                                }
                            })
                            if (c.length == keyAns[keyIdx].length) {
                                dxApp.standerAnswer = 3;
                            }
                        })
                    }
                }
            });
        }
        // 新题型
    } else if (question.type == 9 || question.type == 5) {
        dxApp.typeNineAnalysis = true
        // 新题型回答正确文案
        if (ansStr.length == 1 && ansStr[0] == '' || ansStr.length == 0) {
            dxApp.standerAnswer = 2;
            question.info.forEach(function (item) {
                newStanderAnswer.push(2)
            })

        } else {
            /******
             ***********index是标准答案的索引，取决于页面中有几个题目
             ***********keyIdx是关键字中有/分隔符来说明其由分隔符中的/的几个关键字都是符合的
             ***********indexs是正确答案的关键字答案的个数的索引，取决于有几个关键字
             ******/
            // console.log(standerAnswers)
            standerAnswers.map(function (item, index) {
                var compareArr = item.filter(function (subItem, i) {
                    return strBlank(subItem).trim().toString() == strBlank(ansStr[index].replace(/’/g, "'")).trim().toString();
                })
                if (compareArr.length > 0) {
                    newStanderAnswer.push(0)
                } else {
                    // 关键字提取
                    var keyAns = info[index].keyword.split('/');
                    keyAns.forEach(function (item, keyIdx) {
                        keyAns[keyIdx] = item.split(';')
                    })
                    // var allCompareAns=[];
                    // var c;
                    var compareKey;
                    var b = keyAns.filter(function (item, indexs) {
                        if (item instanceof Array) {
                            // console.log('itemsss',item)
                            var c = item.filter(function (subItem, j) {
                                var newAnsStr = ' ' + strBlank((ansStr[index] || '').replace(/’/g, "'")).trim() + ' ';
                                var newSubIem = ' ' + strBlank(subItem.trim()) + ' ';
                                return newAnsStr.toLowerCase().indexOf(newSubIem.toLowerCase()) !== -1
                            })
                            if (c.length == item.length) {
                                compareKey = true;
                            }
                            // return c
                        }
                        // console.log('sdsxxcxcxcxds',c)
                    })
                    // 未作答与已作答
                    if (ansStr[index]) {
                        if (compareKey) {
                            newStanderAnswer.push(3);
                        } else {
                            newStanderAnswer.push(1);
                        }

                    } else {
                        // 未作答
                        newStanderAnswer.push(2);
                    }

                }
            });
        }
    }else if(question.type == 12) {
        // 整卷新增类型，12，正确与否客户端传  answerType: 0:错 1:对 2:半对 3:未作答
        dxApp.typeNineAnalysis = true
        question.answerType.forEach(function(item, index) {
            if(item == 0) {
                newStanderAnswer.push(1)
            }else if(item == 1) {
                newStanderAnswer.push(0)
            }else if(item == 2) {
                newStanderAnswer.push(3)
            }else if(item == 3) {
                newStanderAnswer.push(2)
            }
        })
    }else if(question.type == 13) {
        dxApp.typeNineAnalysis = true
        // 整卷新增类型 13，整卷批改填空题
        // answerType: 0:错 1:对 2:半对 3:未作答
        // 转换为 newStanderAnswer: 0:正确 1:错误 2:未作答 3:部分正确
        question.answerType.forEach(function(item, index) {
            var answerTypeValue = Number(item); // 转换为数字，兼容字符串类型
            if(answerTypeValue === 0) {
                newStanderAnswer.push(1) // 错误
            }else if(answerTypeValue === 1) {
                newStanderAnswer.push(0) // 正确
            }else if(answerTypeValue === 2) {
                newStanderAnswer.push(3) // 部分正确
            }else if(answerTypeValue === 3) {
                newStanderAnswer.push(2) // 未作答
            } else {
                // 如果 answerType 值不在预期范围内，默认设为未作答
                dxApp.standerAnswer = 2;
            }
        })
        // 设置 standerAnswer 用于显示答案解析（使用第一个答案的状态）
        if (newStanderAnswer.length > 0) {
            if(newStanderAnswer.every(item => item == 0)) {
                dxApp.standerAnswer = 0; // 正确
            } else if(newStanderAnswer.every(item => item == 1)) {
                dxApp.standerAnswer = 1; // 错误
            } else if(newStanderAnswer.every(item => item == 2)) {
                dxApp.standerAnswer = 2; // 未作答
            } else {
                dxApp.standerAnswer = 3; // 部分正确
            } 
        } else {
            // 如果没有 answerType，默认设为未作答
            dxApp.standerAnswer = 2;
        }
    }
    dxApp.questionIndex = Number(index);
    dxApp.question = question;
    dxApp.standerAnswers = standerAnswers;
    dxApp.info = info;
    dxApp.ansStr = ansStr;
    dxApp.newStanderAnswer = newStanderAnswer
    var noShowAll = []
    for (var i = 0; i < standerAnswers.length; i++) {
        noShowAll.push(true)
    }
    dxApp.noShowAll = noShowAll;

    function demo() {
        if(question.type == 12) {
            newStanderAnswer.forEach(function(item, index) {
                if(newStanderAnswer[index] == 0) {
                    $('.input-answer').eq(index).addClass('right-color')
                }else if(newStanderAnswer[index] == 1) {
                    $('.input-answer').eq(index).addClass('incorrect-color')
                }else if(newStanderAnswer[0] == 2) {
                    $('.input-answer').eq(index).addClass('unfinished-color')
                }else if(newStanderAnswer[0] == 3) {
                    $('.input-answer').eq(index).addClass('half-color')
                } 
            })
        }else if(question.type == 13) {
            // type=13 整卷批改填空题，根据 newStanderAnswer 设置样式
            newStanderAnswer.forEach(function(item, index) {
                if(newStanderAnswer[index] == 0) {
                    $('.fill_word').eq(index).addClass('correct')
                }else if(newStanderAnswer[index] == 1) {
                    $('.fill_word').eq(index).addClass('incorrect')
                }else if(newStanderAnswer[index] == 2) {
                    $('.fill_word').eq(index).addClass('unfinished')
                }else if(newStanderAnswer[index] == 3) {
                    $('.fill_word').eq(index).addClass('half-color')
                }
            })
            // type=13 填充答案（与 type=2 一致）
            if (ansStr && ansStr.length > 0) {
                ansStr.map(function (item, index) {
                    $('.fill_word').eq(index).find('input').attr("readonly", true).addClass("answer-disabled-input");
                    $('.fill_word').eq(index).find('input').val(item);
                    $('.fill_word').eq(index).find('input').css('color', '#fff');
                    $('.fill_word').eq(index).find('.fill_number').remove();
                });
            }
        }else if (ansStr.length == 1 && ansStr[0] == '' || ansStr.length == 0) {
            ansStr = [];
            var length = $('.fill_word').length;
            if (question.type == 3) {
                var typeThrArr = [];
                for (var i = 0; i < length; i++) {
                    typeThrArr.push('');
                }
                ansStr.push(typeThrArr);
            } else {
                for (var i = 0; i < length; i++) {
                    ansStr.push('');
                }
            }
            ansStr.map(function (item, index) {
                $('.fill_word').eq(index).addClass('unfinished');
            });
        } else {
            // 标准答案与用户答案做对比
            standerAnswers.map(function (item, index) {
                var compareAns = item.filter(function (subItem, subIdx) {
                    if (question.type == 3) {
                        return strBlank(subItem.toString()) == strBlank(ansStr[0].toString().replace(/’/g, "'"));
                    } else if (question.type == 9 || question.type == 5) {
                        return strBlank(subItem).trim() == strBlank(ansStr[index].replace(/’/g, "'")).trim();
                    } else {
                        return strBlank(subItem) == strBlank((ansStr[index] || '').replace(/’/g, "'"))
                    }
                });
                if (compareAns.length > 0) {
                    // 正确
                    if (question.type == 3) {
                        ansStr[0].map(function (ansStrItem, ansStrIdx) {
                            $('.fill_word').eq(ansStrIdx).addClass('correct');
                        });
                    } else if (question.type == 9) {
                        if (newStanderAnswer[index] == 0) {
                            $('.input-answer').eq(index).addClass('right-color')
                        }
                    }
                    $('.fill_word').eq(index).addClass('correct');
                } else {
                    // 错误
                    if (ansStr[index]) {
                        if (question.type == 9) {
                            if (newStanderAnswer[index] == 1) {
                                $('.input-answer').eq(index).addClass('incorrect-color').removeClass('unfinished-color correct-color')
                            } else if (newStanderAnswer[index] == 3) {
                                $('.input-answer').eq(index).addClass('half-color')
                            }
                        }
                        // 句子填空，有关键字，则有半对
                        else if (question.type == 5) {
                            if (newStanderAnswer[index] == 1) {
                                $('.fill_word').eq(index).addClass('incorrect')
                            } else if (newStanderAnswer[index] == 3) {
                                $('.fill_word').eq(index).addClass('half-color')
                            }
                        }
                        else {
                            $('.fill_word').eq(index).addClass('incorrect');
                        }
                    } else {
                        // 未作答
                        // 旧题型
                        $('.fill_word').eq(index).addClass('unfinished');
                        if (question.type == 9) {
                            if (newStanderAnswer[index] == 2) {
                                // 新题型
                                $(".input-answer").eq(index).addClass('unfinished-color')
                            }
                        }
                    }
                    if (question.type == 3) {
                        ansStr[0].map(function (ansStrItem, ansStrIdx) {
                            if (dxApp.standerAnswer == 3) {
                                $('.fill_word').eq(ansStrIdx).removeClass('incorrect').addClass('half-color');
                            } else {
                                $('.fill_word').eq(ansStrIdx).addClass('incorrect');
                            }
                        });
                    }
                }
            });
        }
        // 用户的答案填充上去
        if (question.type == 3) {
            ansStr[0].map(function (item, index) {
                $('.fill_word').eq(index).find('input').attr("readonly", true).addClass("answer-disabled-input");
                $('.fill_word').eq(index).find('input').val(item);
                $('.fill_word').eq(index).find('input').css('color', '#fff');
                $('.fill_word').eq(index).find('.fill_number').remove();
            });
        } else {
            ansStr.map(function (item, index) {
                $('.fill_word').eq(index).find('input').attr("readonly", true).addClass("answer-disabled-input");
                if (question.type == 1 || question.type == 11) {
                    $('.fill_word').eq(index).find('input').val(item.substr(1));
                } else {
                    $('.fill_word').eq(index).find('input').val(item);
                }
                $('.fill_word').eq(index).find('input').css('color', '#fff');
                $('.fill_word').eq(index).find('.fill_number').remove();
                // 新题型
                $('.input-answer').eq(index).html(item);
                // 设置输入框的长度
                if (question.type == 5) {
                    setInputWidht($('.fill_word').eq(index).find('input'))
                }
            });
        }
    }

    function setAns() {
        if (dxApp.canSetAnswer) {
            demo()
        } else {
            setTimeout(function () {
                setAns()
            }, 10)
        }
    }
    setAns()
    // getRwAnswerStr()
}
// 句子填空，设置输入框宽度
function setInputWidht($_item) {
    var val = $_item.val();
    if (!val) { return }
    var len = val.length;
    var fontSize = parseFloat($_item.css('font-size')) || 16 * window.devicePixelRatio;
    var width = $(this).width();
    var margin = parseFloat($_item.parent().css('margin-left')) || 0;
    var max_width = $_item.parents('p').width() - 2 * margin;
    if (len * fontSize > width) {
        if (len * fontSize > max_width) {
            $_item.parent().css('width', max_width);
        } else {
            $_item.parent().css('width', len * fontSize);
        }
    }
}
// 设置底部高度
function kttb_setDivBotton(height) {
    $('.kttb_setDivBotton').remove();
    var div = $('<div>').addClass('kttb_setDivBotton');
    div.css({ 'width': '100%' });
    div.css('height', height + 'px');
    div.css('min-height', height + 'px');
    div.css('margin', '0');
    div.css('padding', '0');
    $('.container').append(div);
}
// 删除底部高度
function kttb_rmDivBottom() {
    $('.kttb_setDivBotton').remove();
}
// 滚动到顶部
function kttb_scrollTop(height) {
    // $('.container').animate({ scrollTop: height + "px" }, 20);
    $(document, "body", ".container").scrollTop(height)
}
// 获取答案
function getRwAnswerStr() {
    var blank = $(".fill_word_input");
    var choose = $(".choose-con");
    var order = dxApp.info.order;
    // 新题型的答案
    var newAns = $(".input-answer");
    var res = [],
        str = '';
    // 任务型阅读
    if (dxApp.question.type == 10 && location.href.indexOf('rw-index-2') > -1) {
        // 每大题
        $('.fillblank.new').each(function (index_out, item) {
            // 短文填空
            var info_index = $('.fillblank.new').index($('.fillblank.new').eq(index_out));
            var info_item = dxApp.question.info[info_index];
            var new_fillblank = $('.fillblank.new').eq(index_out);
            if (new_fillblank.find('.fill_word_input').length > 0) {
                // var subRes = []
                if (new_fillblank.find('.fill_word_input').length > info_item['sub_question_info'].length) {
                    // 完成句子
                    var obj = {};
                    var value = [];
                    obj.order = info_item['sub_question_info'][0]['order'];
                    new_fillblank.find('.fill_word_input').each(function (index, item) {
                        var val = $(item).val().trim();
                        value.push({
                            value: val
                        })
                    });
                    obj.value = value
                    res.push(obj)
                }
                else {
                    new_fillblank.find('.fill_word_input').each(function (index, item) {
                        var sub_info_index = new_fillblank.find(".fill_word").index($(item).parents('.fill_word'));
                        if (sub_info_index < info_item['sub_question_info'].length) {
                            var szm = $(item).attr("data-szm");
                            var obj = {};
                            var value = [];
                            // if(!info_item['sub_question_info'][sub_info_index]){
                            //     console.log(info_item['sub_question_info'])
                            //     console.log(sub_info_index)
                            //     console.log(info_index)
                            // }
                            obj.order = info_item['sub_question_info'][sub_info_index]['order'];
                            var val = $(item).val().trim();
                            if (szm !== undefined && typeof szm === 'string' && val !== '') {
                                val = szm + val;
                            }
                            var subObj = {};
                            subObj.value = val;
                            value.push(subObj);
                            obj.value = value;
                            res.push(obj);
                            // subRes.push(obj);
                        }
                    });
                }
                // res.push(subRes)
            }
            // 选择题
            // if (new_fillblank.find('.choose-con').length > 0) {
            //     var subRes = [];
            //     new_fillblank.find('.choose-con').each(function(index, item) {
            //         var sub_info_index = $(item).parents('div').eq(0).index($(item).parents('.fill_word'));
            //         var obj = {};
            //         var value = [];
            //         obj.order = info_item['sub_question_info'][sub_info_index]['order'];
            //         var val = '';
            //         var active_item = choose.find('.choose-item.active');
            //         if (active_item.length > 0) {
            //             val = $(active_item).attr("data-option");
            //         }
            //         var subObj = {};
            //         subObj.value = val;
            //         value.push(subObj);
            //         obj.value = value;
            //             res.push(obj);
            //         subRes.push(obj);
            //     })
            //     res.push(subRes)
            // }
            // 回答题textare
            if (new_fillblank.find('.input-answer').length > 0) {
                // var subRes = [];
                new_fillblank.find('.input-answer').each(function (index, item) {
                    var sub_info_index = new_fillblank.find('.import').index($(item).parents('.import'));
                    if (sub_info_index < info_item['sub_question_info'].length) {
                        var obj = {};
                        obj.order = info_item['sub_question_info'][sub_info_index]['order'];
                        obj.value = []
                        if ($(item).val() == '请输入答案') {
                            obj.value.push({ value: '' })
                        } else {
                            obj.value.push({ value: $(item).val() })
                        }
                        res.push(obj);
                        // subRes.push(obj)
                    }
                })
                // res.push(subRes)
            }
        })
    } else {
        // 填空题
        if (blank.length > 0) {
            // 一个quetion多个info
            if (dxApp.question.info.length > 1) {
                blank.each(function (index, item) {
                    var szm = $(item).attr("data-szm");
                    var obj = {};
                    var value = [];
                    obj.order = index;
                    var val = $(item).val().trim();
                    if (szm !== undefined && typeof szm === 'string' && val !== '') {
                        val = szm + val;
                    }
                    var subObj = {};
                    subObj.value = val;
                    value.push(subObj);
                    obj.value = value;
                    res.push(obj);
                });
            } else {
                // 单个info
                var obj = {};
                var value = [];
                obj.order = order;
                blank.each(function (index, item) {
                    var szm = $(item).attr("data-szm");
                    var val = $(item).val().trim();
                    if (szm !== undefined && typeof szm === 'string' && val !== '') {
                        val = szm + val;
                    }
                    var subObj = {};
                    subObj.value = val;
                    value.push(subObj);
                });
                obj.value = value;
                res.push(obj);
            }
        }
        // 选择题，只有单个info的
        else if (choose.length > 0) {
            var obj = {};
            var value = [];
            obj.order = order;
            var val = '';
            var active_item = choose.find('.choose-item.active');
            if (active_item.length > 0) {
                val = $(active_item).attr("data-option");
            }
            var subObj = {};
            subObj.value = val;
            value.push(subObj);
            obj.value = value;
            res.push(obj);
        }
        // 新题型的答案
        else if (newAns.length > 0) {
            var res = []
            newAns.each(function (index, item) {
                var obj = {};
                obj.order = index;
                obj.value = []
                if ($(item).val() == '请输入答案') {
                    obj.value.push({ value: '' })
                } else {
                    obj.value.push({ value: $(item).val() })
                    // obj.value.value=$(item).html();
                }
                res.push(obj)
            })

        }
    }
    console.log(res)
    str = JSON.stringify(res);
    if (webviewSystem === 2) {
        window.webInteraction.getRwWord(str);
    } else if (webviewSystem === 1) {
        location.href = "ets://get_rw_result?result=" + str;
    }
}
// 设置答案
function setRwAnswerStr(str, errorType) {
    if (typeof str == "string") {
        var value = JSON.parse(str);
    } else {
        var value = str
    }
    // 任务型阅读
    if (dxApp.question.type == 10) {
        if (location.href.indexOf('rw-index-1') > -1) {
            // 子面板
            var choose = $(".choose-con");
            var val = value[0];
            if (val !== '') {
                choose.find(".choose-item").removeClass("active");
                choose.find(".choose-item[data-option=" + val + "]").addClass("active");
            }
        } else {
            // 主面板
            for (var index = 0; index < $('.fillblank.new').length; index++) {
                // 填空选择题
                if ($('.fillblank.new').eq(index).find('.fill_word_input').length > 0) {
                    if (value[index]) {
                        value[index].forEach(function (subItm, subIdx) {
                            if (subItm !== '') {
                                var szm = $('.fillblank.new').eq(index).find('.fill_word_input').eq(subIdx).attr("data-szm");
                                var len = 0;
                                if (szm !== undefined && typeof szm === 'string') {
                                    len = szm.length;
                                }
                                // if(!subItm.substring && errorType==1){
                                //     subItm = subItm.userAns&&subItm.userAns.toString() || ''
                                // }
                                subItm = subItm.substring(len, subItm.length);
                                $('.fillblank.new').eq(index).find('.fill_word_input').eq(subIdx).val(subItm).parents('.fill_word').addClass('fill_word_lightblue');
                            }
                        })
                    }
                }
                // 回答题
                if ($('.fillblank.new').eq(index).find('.input-answer').length > 0) {
                    $('.fillblank.new').eq(index).find('.input-answer').each(function (subIdx, subItm) {
                        if (value[index]) {
                            if (typeof value[index][subIdx] != 'undefined') {
                                value[index][subIdx] = value[index][subIdx].replace(/<br \/>/g, '\n')
                                $(subItm).val(value[index][subIdx])
                                $(subItm).prev().find('span').html(value[index][subIdx])
                            }
                        }
                    })
                }
            }
        }
    } else {
        var blank = $(".fill_word_input");
        var choose = $(".choose-con");
        var newAns = $(".input-answer")
        if (blank.length > 0) {
            $(value).each(function (index, item) {
                // console.log(item)
                if (item !== '') {
                    var szm = $(blank[index]).attr("data-szm");
                    var len = 0;
                    if (szm !== undefined && typeof szm === 'string') {
                        len = szm.length;
                    }
                    item = item.substring(len, item.length);
                    $(blank[index]).val(item).parents('.fill_word').addClass('fill_word_lightblue');
                }
            });
        } else if (choose.length > 0) {
            var val = value[0];
            if (val !== '') {
                choose.find(".choose-item").removeClass("active");
                choose.find(".choose-item[data-option=" + val + "]").addClass("active");
            }
        } else if (newAns.length > 0) {
            newAns.each(function (index, item) {
                if (value[index]) {
                    value[index] = value[index].replace(/<br \/>/g, '\n')
                    $(item).val(value[index])
                    $(item).prev().find('span').html(value[index])
                }
            })
        }
    }
    // 如果是错题
    if (errorType && errorType == 1) {
        setTimeout(function () {
            $('.fill_word_lightblue').css({ "background-color": "#FFFFFF", 'border-color': '#E6E6E6', "color": "#3C3C3C" })
            $('.fill_word_lightblue input').css({ "color": "#3C3C3C" }).attr({ 'readOnly': true, 'data-error': 1 });
            $('.import').each(function (index, item) {
                if ($(item).find('pre').text().trim() != '') {
                    $(item).find('textarea').attr({ 'readOnly': true, 'data-error': 1 }).css('color', '#979797');
                    $(item).find('.number').css('color', '#979797');
                }
            })
        })
        if (str && str.length > 0 && value.lenght > 0) {
            $('.choose-con').addClass('disabled-hasicon disabled')
        }
    }
    $('input').each(function (index, item) {
        setInputWidht($(item))
    })
}
// 设置答案
function setRwAnswerForIndex(idx, str) {
    if (dxApp.question.type == 10) {
        var blank = $('input[data-order=' + idx + ']');
        if (blank.length > 0) {
            if (str !== '') {
                var szm = blank.eq(0).attr("data-szm");
                var len = 0;
                if (szm !== undefined && typeof szm === 'string') {
                    len = szm.length;
                }
                str = str.substring(len, str.length);
                blank.eq(0).val(str).parents('.fill_word').addClass('fill_word_lightblue');
            }
        }
    } else {
        var blank = $(".fill_word_input");
        if (blank.length > 0) {
            if (str !== '') {
                var szm = $(blank[idx]).attr("data-szm");
                var len = 0;
                if (szm !== undefined && typeof szm === 'string') {
                    len = szm.length;
                }
                str = str.substring(len, str.length);
                $(blank[idx]).val(str).parents('.fill_word').addClass('fill_word_lightblue');
            }
        }
    }
}

// input失去焦点
function inputBlur() {
    $("input").blur();
    $("textarea").blur();

}

function setRwFocusForIndex(index) {
    if (dxApp.question.type == 10) {
        var target = $("input[data-order=" + index + "], textarea[data-order=" + index + "]");
        if (target.length > 0) {
            $("input, textarea").blur();
            target.eq(0).click();
        }
    } else {
        var blank = $(".fill_word_input");
        if (blank.length > 0) {
            $('.fill_word_input').eq(index).click();
        }
    }
}


// 任务型阅读对比答案
function comTaskReading(question, index, str, panel, info, bigIndex) {
    if (dxApp) {
        dxApp.canSetAnswer = false;
        dxApp.answerPage = false;
    }
    if (typeof question === 'string') {
        question = question.replace(/\r\n|\r|\n|↵|\\r\\n|\\r|\\n/g, '<br />')
        question = JSON.parse(question);
    }
    dxApp.question = question;
    if (typeof info === 'string') {
        info = info.replace(/\r\n|\r|\n|↵|\\r\\n|\\r|\\n/g, '<br />')
        info = JSON.parse(info);
    }
    /* 0 回答正确
    /* 1 回答错误
    /* 2 未作答
    * 3 部分正确*/
    dxApp.questionIndex = Number(index);
    var value = [];
    if (typeof str == "string") {
        value = JSON.parse(str);
    } else {
        value = str
    }
    if (panel == 'main') {
        var tenScoreArr = [];
        var noShowAll = [];

        // 得分重组
        value.forEach(function (item, index) {
            var subTenScoreArr = []
            item.forEach(function (subItem, subIdx) {
                var haveUsrAns = subItem.userAns.filter(function (filItem, filIdx) {
                    return filItem !== ''
                })
                if (haveUsrAns.length > 0) {
                    if (subItem.score == 0) {
                        subTenScoreArr.push(1)
                    } else if (subItem.score == subItem.totalScore) {
                        subTenScoreArr.push(0)
                    } else {
                        subTenScoreArr.push(3)
                    }
                } else {
                    // 未作答
                    subTenScoreArr.push(2)
                }
            })
            tenScoreArr.push(subTenScoreArr)
            noShowAll.push(true)
        })

        dxApp.typeTenAnalysis = true;
        dxApp.typeNineAnalysis = false
        dxApp.tenScoreArr = tenScoreArr
        dxApp.noShowAll = noShowAll

        // 答案填充
        var comTaskReading_setAS = function () {
            if (dxApp.canSetAnswer) {
                for (var index = 0; index < $('.fillblank.new').length; index++) {
                    // 填空选择题
                    if ($('.fillblank.new').eq(index).find('.fill_word_input').length > 0) {
                        value[index].forEach(function (subItm, subIdx) {
                            // 一道题对应一个空
                            if (subItm.userAns.length == 1) {
                                var szm = $('.fillblank.new').eq(index).find('.fill_word_input').eq(subIdx).attr("data-szm");
                                var len = 0;
                                if (szm !== undefined && typeof szm === 'string') {
                                    len = szm.length;
                                    if (question['info'][index]['type'] == 10) {
                                        $('.fillblank.new').eq(index).find('.fill_word').addClass('fill_word_sel');
                                    }
                                }
                                subItm = subItm.userAns[0].substring(len, subItm.userAns[0].length);
                                tenScoreArr[index][subIdx] == 0 ? $('.fillblank.new').eq(index).find('.fill_word').eq(subIdx).addClass('correct') :
                                    tenScoreArr[index][subIdx] == 1 ? $('.fillblank.new').eq(index).find('.fill_word').eq(subIdx).addClass('incorrect') :
                                        $('.fillblank.new').eq(index).find('.fill_word').eq(subIdx).addClass('unfinished')
                                $('.fillblank.new').eq(index).find('.fill_word_input').eq(subIdx).val(subItm);
                                $('.fillblank.new').eq(index).find('.fill_word_input').eq(subIdx).css('color', '#fff');
                                $('.fillblank.new').eq(index).find('.fill_number').remove();
                                $('.fillblank.new').eq(index).find('.fill_word_input').attr("readonly", true).addClass("answer-disabled-input");
                            } else {
                                //一道题对应多个空
                                subItm.userAns.forEach(function (comSenItm, comSenIdx) {
                                    var szm = $('.fillblank.new').eq(index).find('.fill_word_input').eq(comSenIdx).attr("data-szm");
                                    var len = 0;
                                    if (szm !== undefined && typeof szm === 'string') {
                                        len = szm.length;
                                    }
                                    subItm = comSenItm.substring(len, comSenItm.length);
                                    if (tenScoreArr[index][subIdx] == 0) {
                                        $('.fillblank.new').eq(index).find('.fill_word').eq(comSenIdx).addClass('correct')
                                    } else if (tenScoreArr[index][subIdx] == 1) {
                                        $('.fillblank.new').eq(index).find('.fill_word').eq(comSenIdx).addClass('incorrect')
                                    } else if (tenScoreArr[index][subIdx] == 2) {
                                        $('.fillblank.new').eq(index).find('.fill_word').eq(comSenIdx).addClass('unfinished')
                                        // $('.fillblank.new').eq(index).find('.fill_word_input').eq(comSenIdx).val(comSenItm);
                                        // $('.fillblank.new').eq(index).find('.fill_word_input').eq(comSenIdx).css('color', '#fff');
                                        // $('.fillblank.new').eq(index).find('.fill_number').remove();
                                        // $('.fillblank.new').eq(index).find('.fill_word_input').eq(comSenIdx).attr("readonly",  true).addClass("answer-disabled-input");
                                    } else {
                                        $('.fillblank.new').eq(index).find('.fill_word').eq(comSenIdx).addClass('half-color');
                                        // $('.fillblank.new').eq(index).find('.fill_word_input').eq(comSenIdx).val(comSenItm);
                                        // $('.fillblank.new').eq(index).find('.fill_word_input').eq(comSenIdx).css('color', '#fff');
                                        // $('.fillblank.new').eq(index).find('.fill_number').remove();
                                        // $('.fillblank.new').eq(index).find('.fill_word_input').eq(comSenIdx).attr("readonly",  true).addClass("answer-disabled-input");
                                    }
                                    $('.fillblank.new').eq(index).find('.fill_word_input').eq(comSenIdx).val(comSenItm);
                                    $('.fillblank.new').eq(index).find('.fill_word_input').eq(comSenIdx).css('color', '#fff');
                                    $('.fillblank.new').eq(index).find('.fill_number').remove();
                                    $('.fillblank.new').eq(index).find('.fill_word_input').eq(comSenIdx).attr("readonly", true).addClass("answer-disabled-input");
                                })
                            }
                        })
                    }
                    // 回答题
                    if ($('.fillblank.new').eq(index).find('.input-answer').length > 0) {
                        $('.fillblank.new').eq(index).find('.input-answer').each(function (subIdx, subItm) {
                            $(".input-answer").eq(index).addClass('unfinished-color')
                            tenScoreArr[index][subIdx] == 0 ? $('.fillblank.new').eq(index).find('.input-answer').eq(subIdx).addClass('correct-color').removeClass('unfinished-color incorrect-color') :
                                tenScoreArr[index][subIdx] == 1 ? $('.fillblank.new').eq(index).find('.input-answer').eq(subIdx).addClass('incorrect-color').removeClass('unfinished-color correct-color') :
                                    tenScoreArr[index][subIdx] == 2 ? $('.fillblank.new').eq(index).find('.input-answer').eq(subIdx).addClass('unfinished-color') :
                                        $('.fillblank.new').eq(index).find('.input-answer').eq(subIdx).addClass('half-color')
                            if (value[index][subIdx]) {
                                value[index][subIdx].userAns[0] = value[index][subIdx].userAns[0].replace(/<br \/>/g, '\n')
                                $(subItm).html(value[index][subIdx].userAns[0])
                            }
                        })
                    }
                }
            } else {
                setTimeout(comTaskReading_setAS, 100)
            }
        }
        comTaskReading_setAS()
        // dxApp.answerPage = true
    } else {
        // 子面板
        var haveUsrAns = value.userAns && value.userAns.filter(function (item) {
            return item !== ''
        });
        if (haveUsrAns.length == 0) {
            dxApp.standerAnswer = 2
        } else {
            if (value.score == value.totalScore) {
                dxApp.standerAnswer = 0
            } else {
                dxApp.standerAnswer = 1
            }
        }
        dxApp.userAns = value.userAns[0]
        // console.log(info)
        dxApp.info = info;
        dxApp.standerAns = info.answer
        dxApp.answerPage = true
        dxApp.bigIndex = bigIndex
        setTimeout(function () {
            $('.choose-item').removeClass('active')
        }, 0)
    }

}

// 下一空
function goNextBlank() {
    var dom = $('input:focus , textarea:focus').eq(0);
    var allDom = $('input, textarea');
    var i = allDom.index(dom);
    // console.log(i)
    if (i + 1 >= allDom.length) {
        // scrollNextPage()
        if (webviewSystem === 2) {
            // window.webInteraction.scrollNextPage();
            if(window.webInteraction && window.webInteraction.onLastBlank) {
                window.webInteraction.onLastBlank()
            }
        } else if (webviewSystem === 1) {
            // location.href = "ets://get_rw_result?result=" + str;
        }
    } else {
        dom.blur();
        allDom.eq(i + 1).focus();
        // 往上滚动20px
        // var scrollTop = $('#dxApp').scrollTop();
        // $('#dxApp').scrollTop(scrollTop+50);
    }
}

/************** app调用方法  结束 **************/