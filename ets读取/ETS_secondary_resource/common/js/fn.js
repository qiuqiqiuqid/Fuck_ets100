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
function phonogramShowClock(val) {
	dxApp.isNew = 1;
	// if (val == 1) {
	// 	dxApp.cardType = 'learn';
	// 	dxApp.isLock = 1;
	// } else {
	// 	dxApp.cardType = 'use';
	// 	dxApp.isLock = 0
	// }
}
function setLockData(data) {
	dxApp.isNew = 1;

	data.forEach(function(item, index) {

		// 有锁
		if (item.lock == 1) {
			$('#' + item.id).addClass('locked')
		}
	})
}
/**设置总分数**/
/**
 **	例如 60
 **/
function alertMsg(msg) {
	alert(msg);
}

function setWordText(word_txt) {
	$(".word_txt").nextAll().remove();
	$(".word_txt").html(word_txt);
}

function setEvent() {
	$(".word").click(function() {
		var word = $(this).text();
		var x = $(this).offset().top;
		var y = $(this).offset().left;
		window.webInteract.showPopWindow(word, x, y);
	});
}

function setScore(maxScore, currScore) {
	var ration = currScore / maxScore;
	if (ration >= 0 && ration < 0.6) {
		$(".score_con")[0].className = "score_con red"
	} else if (ration >= 0.6 && ration < 0.8) {
		$(".score_con")[0].className = "score_con yellow"
	} else if (ration >= 0.8 && ration <= 1) {
		$(".score_con")[0].className = "score_con green"
	};
	$("#score").text(currScore);
	$("#total-score").text(maxScore);
	// ../../../common/images/yellow_star.png
	//$(".star_img").attr("src", "../../../common/images/grey_star.png");
	var base = maxScore / 5;
	var yellow_star_n = Math.round(currScore / base);
	for (var i = 0; i < 5; i++) {
		if (i < yellow_star_n) {
			$(".star_img")[i].src = "../../../common/images/yellow_star.png";
		} else {
			$(".star_img")[i].src = "../../../common/images/grey_star.png";
		}
	}
	$(".result_con").show();
}
/*显示得分和星星*/
function showScore(maxScore, currScore) {
	var ration = (currScore * 100) / (maxScore * 100);
	if (ration >= 0 && ration < 0.6) {
		$(".score_con")[0].className = "score_con red"
	} else if (ration >= 0.6 && ration < 0.8) {
		$(".score_con")[0].className = "score_con yellow"
	} else if (ration >= 0.8 && ration <= 1) {
		$(".score_con")[0].className = "score_con green"
	};
	$("#score").text(currScore);
	$("#total-score").text(maxScore);
	if (ration < 0.1) {
		$('.star_img').attr('src', "../../../common/images/grey_star.png");
	} else if (ration >= 0.1 && ration < 0.2) {
		$('.star_img:eq(0)').attr('src', "../../../common/images/half_star.png");
		$('.star_img:gt(0)').attr('src', "../../../common/images/grey_star.png");
	} else if (ration >= 0.2 && ration < 0.3) {
		$('.star_img:eq(0)').attr('src', "../../../common/images/yellow_star.png");
		$('.star_img:gt(0)').attr('src', "../../../common/images/grey_star.png");
	} else if (ration >= 0.3 && ration < 0.4) {
		$('.star_img:eq(0)').attr('src', "../../../common/images/yellow_star.png");
		$('.star_img:eq(1)').attr('src', "../../../common/images/half_star.png");
		$('.star_img:gt(1)').attr('src', "../../../common/images/grey_star.png");
	} else if (ration >= 0.4 && ration < 0.5) {
		$('.star_img:lt(2)').attr('src', "../../../common/images/yellow_star.png");
		$('.star_img:gt(1)').attr('src', "../../../common/images/grey_star.png");
	} else if (ration >= 0.5 && ration < 0.6) {
		$('.star_img:lt(2)').attr('src', "../../../common/images/yellow_star.png");
		$('.star_img:eq(2)').attr('src', "../../../common/images/half_star.png");
		$('.star_img:gt(2)').attr('src', "../../../common/images/grey_star.png");
	} else if (ration >= 0.6 && ration < 0.7) {
		$('.star_img:lt(3)').attr('src', "../../../common/images/yellow_star.png");
		$('.star_img:gt(2)').attr('src', "../../../common/images/grey_star.png");
	} else if (ration >= 0.7 && ration < 0.8) {
		$('.star_img:eq(3)').attr('src', "../../../common/images/half_star.png");
		$('.star_img:eq(4)').attr('src', "../../../common/images/grey_star.png");
		$('.star_img:lt(3)').attr('src', "../../../common/images/yellow_star.png");
	} else if (ration >= 0.8 && ration < 0.9) {
		$('.star_img:eq(4)').attr('src', "../../../common/images/grey_star.png");
		$('.star_img:lt(4)').attr('src', "../../../common/images/yellow_star.png");
	} else if (ration >= 0.9 && ration < 0.95) {
		$('.star_img:eq(4)').attr('src', "../../../common/images/half_star.png");
		$('.star_img:lt(4)').attr('src', "../../../common/images/yellow_star.png");
	} else if (ration >= 0.95 && ration <= 1) {
		$('.star_img').attr('src', "../../../common/images/yellow_star.png");
	};
	$(".result_con").show();
}

function setScoreDetail() {
	var arr = arguments;
	// var dwd_item_length = $(".dwd_item").length
	// if(arr.length == 4 && dwd_item_length == 3){
	// 	var h = '<div class="dwd_item">'+ 
	// 				'<div class="dwd_name">标准度</div> '+
	// 				'<div>  '+
	// 					'<span class="dwd_flu"></span> '+
	// 					'<span class="dwd_fen">分</span>  '+
	// 				'</div>  '+
	// 			'</div> ' ;
	// 	$(".dwd_item").parent().css({"display":"block","margin":"0.375rem 0.6rem","width":"initial"}).append(h);
	// }
	// for (var i = 0; i < arguments.length; i++) {
	// 	$('.dwd_flu:eq(' + i + ')').text(arr[i]);
	// }
	// if($(".dwd_item").length > 4){
	// 	$(".dwd_item").each(function(index, item){
	// 		if(index > 3){
	// 			$(item).remove()
	// 		}
	// 	})
	// }
	// $(".score_con,.score_con .font12,#total-score").css("font-size","0.5rem");
	// $(".score_con #score").css("font-size", "1.4rem");
	if (arr.length == 4) {
		$(".dwd-item").removeClass('none')
	}else {
		$(".dwd-item").eq(3).remove()
	}
	for (var i = 0; i < arr.length; i++) {
		var item = arr[i];
		$(".dwd-item").eq(i).find(".num").text(item.score);
	}
	$('.dwd-analysis').removeClass('none');
}
/*
function atonum(str){
	switch(str){
		case "A":
			return 1;
			break;
		case "B":
			return 2;
			break;
		case "C":
			return 3;
			break;
		case "D":
			return 4;
			break;
		default:
			return false
	}
}*/

function atonum(str) {
	if (str && $.trim(str).length == 1) {
		str = str.toUpperCase();
		var retVal = str.charCodeAt(0) - 'A'.charCodeAt(0) + 1;
		if (retVal > 0 && retVal < 27) {
			return retVal;
		}
	}
	return false;
}


function getChapterOrgText() {
	var content = $(".original_con");
	if (content.length < 1) {
		content = $(".title");
	}
	if (content.length > 0) {
		var text;
		content.each(function getText(i) {
			if (i == 0) {
				text = $(this).text().trim();
				window.webInteract.setHtmlChapterText1(text);
			}
		});
	}
}

function setChapterOrgText(srcHtml) {
	var content = $(".original_con");
	if (content.length < 1) {
		content = $(".title");
		srcHtml = "<br/>" + srcHtml;
	}
	if (content.length > 0) {
		content.each(function getText(i) {
			if (i == 0) {
				text = $(this).html(srcHtml);
			}
		});
	}
}

function getChapterTranslateText() {
	var translateArrText = [];
	$(".translate_con .original_w_symbol").each(
		function getText(i) {
			translateArrText[i] = $(this).text().trim();
		});
	window.webInteract.setHtmlChapterText2(translateArrText.join("$#-"));
}

function setChapterTranslateText() {
	var textArr = arguments; // 获取文本内容
	var len = textArr.length;
	$(".translate_con .original_w_symbol").each(
		function getText(i) {
			if (len > i) {
				$(this).html(textArr[i]);
			}
		});
}


/****设置每道题的得分，不适用于长对话理解**/
/*
 ** 	number: 题号,
 **	score:  用户分数,
 ** 	all_score:  总分
 **	stander_ans: 正确答案,如果不是选择题，则不传此字段
 **	user_ans: 用户答案，如果不是选择题，则不传此字段
 **	例如：  短对话理解时----	[{number:"42737_1",score:0,all_score:5,stander_ans:"A",user_ans:"B"},{}]
 **			其他只需要设置分数的： [{number:"42737_1",score:3,all_score:5}]		
 */
function setScoreAnswer(arr) {
	var arr = typeof arr == 'string' ? JSON.parse(arr) : arr;
	$(arr).each(function(index, item) {
		var number = item.number,
			score = item.score,
			all_score = item.all_score,
			stander_ans = item.stander_ans,
			user_ans = item.user_ans;
		var q_score = $("#" + number + " .q_score");
		q_score.find(".q_score_con").text('得分：' + score + "分/" + all_score + "分");
		var ration = score / all_score;
		if (ration >= 0 && ration < 0.6) {
			$("#" + number).find('.q_score')[0].className = "q_score red";
		} else if (ration >= 0.6 && ration < 0.8) {
			$("#" + number).find('.q_score')[0].className = "q_score yellow";
		} else if (ration >= 0.8 && ration <= 1) {
			$("#" + number).find('.q_score')[0].className = "q_score green";
		};
	})
}

function setChooseAnswer(arr) {
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
				var num = atonum(stander_ans) - 1;
				$($("#" + number).find(".eorr")[num]).attr("src", "../../../common/images/right1.png").parent().addClass("rightPic");
			} else {
				$("#" + number).find(".question1_title").css("color", "#e65151");
				var r_num = atonum(stander_ans) - 1;
				var u_num = atonum(user_ans) - 1;
				$($("#" + number).find(".eorr")[r_num]).attr("src", "../../../common/images/right1.png");
				$($("#" + number).find(".eorr")[u_num]).parent().addClass("errorPic");
			};
		} else if ($('#' + number).hasClass('question2') || $('#' + number).parent().hasClass('question2')) {
			//短对话
			var q_score = $("#" + number);
			q_score.find(".q_score_con").text('得分：' + score + "分/" + all_score + "分");
			if (stander_ans == user_ans) {
				$('#' + number).find('.q_score_con').parent().removeClass('red').addClass('green');
			};
			var choose2 = $("#" + number + " .choose2");
			var right_num = atonum(stander_ans) - 1;
			$(choose2[right_num])[0].className = "choose2 rightCh green";
			if (stander_ans != user_ans && user_ans !== '') {
				var user_num = atonum(user_ans) - 1;
				$(choose2[user_num])[0].className = "choose2 errorCh red";
			};
		}
	})
}


/**播放标准答案录音**/
/*
 ** 	id: 题号
 */
function stander_audio(id) {
	var ua = navigator.userAgent.toLowerCase();
	if (/iphone|ipad|ipod/.test(ua)) {
		location.href = "ets://stander_audio?recordId=" + id;
	} else if (/android/.test(ua)) {
		try {
            window.webInteraction.playOrgAudio(id);
        } catch (e) {}
        window.webInteraction.playOrgAudio(id, '');
	}
}

function getSentenceText1(isHtml) {
	var content = $(".word_con_1 .word_txt");
	if (content.length < 1) {
		content = $(".word_con_1 .se_txt");
	}
	if (content.length > 0) {
		if (isHtml) {
			var text = content.html();
			getHtml1Content(text);
		} else {
			var text = content.text().trim();
			getHtml1Content(text);
		}
	}
}

function getSentenceText2() {
	var content = $(".word_con_2 .word_txt");
	if (content.length < 1) {
		content = $(".word_con_1 .se_txt");
	}
	if (content.length > 0) {
		var text;
		content.each(function getText(i) {
			if (i == 0) {
				text = $(this).text().trim();
				getHtml2Content(text);
			}
		})
	}
}

function getHtml1Content(text) {
	window.webInteract.setHtmlText1(text);
}

function getHtml2Content(text) {
	window.webInteract.setHtmlText2(text);
}

function setSentenceText1(srcText) {
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
}

function setSentenceText2(srcText, isShow) {
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

}



/**播放用户录音**/
/**
 ***		id: 题号
 ***		fileId: 录音名
 **/
function user_audio(id, fileId) {
	var ua = navigator.userAgent.toLowerCase();
	if (/iphone|ipad|ipod/.test(ua)) {
		location.href = "ets://user_audio?qid=" + id + "&recordId=" + fileId;
	} else if (/android/.test(ua)) {
		window.webInteraction.playRecordAudio(id, fileId);
	}
}

/*模考播放完录音后按钮设置回白色背景*/
function reset_mkButtonView(str) {
	var that = $('.audio_con .' + str);
	var src = '';
	$(".audio_con .playing").removeClass("playing");
	if (str == 'stander_record') {
		src = '../../../common/images/ans_record_icon.png';
		that.find('span').text('原音');
	} else {
		src = '../../../common/images/ans_my_record.png';
		that.find('span').text('录音');
	};
	that.css({ "background-color": '#fff', "color": "#3ad56f" });
	that.find('img').attr('src', src);
}

/*显示单词信息   包括短语和句子*/
/****
 ****  word_txt: 显示的单词   对应设计稿： person  
 ****  word_info: 单词的详细信息   对应设计稿：[英] ['p3':sn]  n,  人
 ****  都是字符串类型
 **/
function setWord(word_txt, word_info) {
	$(".word_txt").html(word_txt);
	$(".word_info").html(word_info);
}

function setWordText(word_txt) {
	$(".word_txt").html(word_txt);
	$(".word_info").show();
}

function showWordTextOrg() {
	$(".word_txt").hide();
	$(".word_txt_org").show();
	$(".word_info").show();
}

function showNote() {
	$(".word_con_1").hide()
	$(".word_con_2").show();
}

function hidenNote() {
	$(".word_con_2").hide()
	$(".word_con_1").show();
}

/*显示单词评测错误*/
/****
 ****参数为string；
 **** 可以传多个参数，例如： showWordError("a","b","c")
 **/
function showWordError() {
	var prama_len = arguments.length;
	$(".word_eval_result_con .in_b").empty();
	for (var i = 0; i < prama_len; i++) {
		var p = $("<p>");
		var text = arguments[i];
		p.html(text);
		$(".word_eval_result_con .in_b").append(p);
	};
	$(".word_eval_result_con").show();
	// setMainCenter();
}

/*翻译和注释同时显示
function showTranslate() {
    $(".translate_con").show().siblings().hide();
}
function showTranslate() {
	if($.trim($(".translate_con").text()).length>0){
		$(".translate_con").show().siblings().hide();
	}else {
		showOriginal_w_symbol() ;
	}
}
*/
function showTranslate(isShow) {
	if ($.trim($(".translate_con").text()).length > 0) {
		$(".translate_con").show().siblings().hide();
	} else {
		if (isShow) {
			showOriginal_w_symbol();
		}
	}
}

function getWordText() {
	var text = $(".word_txt").html();
	window.webInteraction.getWordText(text);
}

/*只显示原文*/
function showOriginal() {
	$(".original_con").show().siblings().hide();
}
/*只显示注释*/
function showOriginal_w_symbol() {
	$(".original_w_symbol_con").show().siblings().hide();
}


/**查看大图图片**/
$(document).on("click", ".show-big-img", function(e) {
	var img_url = $(this).find("img").attr("src");
	/*执行ios或安卓相应的函数*/
	var ua = navigator.userAgent.toLowerCase();
	if (/iphone|ipad|ipod/.test(ua)) {
		/*ios*/
		location.href = "ets://show_big_img?imgUrl=" + img_url;
	} else if (/android/.test(ua)) {
		/*android*/
		showBigImg(img_url);
	}
})

function showBigImg(img_url) {
	window.webInteraction.showBigImg(img_url + "");
}

function getReadingAloud() {
	var content = $.trim($(".test_con .recordText_con").html());
	window.webInteraction.getReadingAloudContent(content + "");
}


//课堂同步／／／／／／／／／／／／／／
// 判断是否有答案解析
function showOrHideAnalysis() {
	var ana = $('.analysis');
	var text = ana.text().trim();
	if (text === '答案解析：' || text === '答案解析') {
		ana.hide();
	} else {
		ana.show();
	};
}
// 选择题
function kttb_getChoice(childId, position) {
	var arr = [],
		all_l, n = 0;
	if ($(".choose1").length !== 0) {
		$(".choose1_selected").each(function(index, item) {
			n++;
			var i = $(item).index();
			var a = {};
			a.id = $(this).parent().parent().parent().attr("id");
			a.answer = numtoa(i);
			arr.push(a);
		})
	}
	if ($(".choose2").length !== 0) {
		$(".choose2_selected").each(function(index, item) {
			n++;
			var i = $(item).index();
			var a = {};
			a.id = $(this).parent().parent().attr("id");
			a.answer = numtoa(i);
			arr.push(a);
		})
	};
	all_l = $(".question1").length + $('.question2').length;

	//console.log(arr) ;

	var ua = navigator.userAgent.toLowerCase();
	if (/iphone|ipad|ipod/.test(ua)) {
		location.href = "ets://get_choice?arr=" + JSON.stringify(arr) + "&childId=" + childId + "&questCount=" + all_l + "&answerCount=" + n;
	} else if (/android/.test(ua)) {
		window.webInteraction.kttb_getChoice(JSON.stringify(arr) + "", childId + "", all_l, n, position);
	}
}

function kttb_setChoice(str) {
	var arr = str.split(",");
	$(arr).each(function(index, item) {
		if ($("#" + item).hasClass("choose1")) {
			$("#" + item).addClass("choose1_selected").siblings().removeClass("choose1_selected");
		} else {
			$("#" + item).addClass("choose2_selected").siblings().removeClass("choose2_selected");
		}
	});
	showOrHideAnalysis();
}

function kttb_setChooseAnswer(arrStr) {
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
		var user_num = atonum(user_ans) - 1;
		var stander_num = atonum(stander_ans) - 1;
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
	showOrHideAnalysis();
}

// 设置单词
function kttb_setWord(word, b) {
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
	showOrHideAnalysis();
}

function kttb_rmDivBottom() {
	$('.kttb_setDivBotton').remove();
}
// 清空单词
function kttb_resetWord() {
	$('.write_result').hide();
	$('.write_input_con').show();
	$('.write_input').val('');
	$('.analysis').hide();
	$('.next_icon').removeClass('next_icon_right next_icon_error');
}

function kttb_getWord() {
	var id = $('.write_input').attr('id').trim();
	var word = $('.write_input').val().trim();
	var ua = navigator.userAgent.toLowerCase();
	if (/iphone|ipad|ipod/.test(ua)) {
		location.href = "ets://get_word?qid=" + id + "&word=" + word;
	} else if (/android/.test(ua)) {
		window.webInteraction.getWord(id, word);
	}
}
// 判断题提交后设置答案
// user_answer:   0  或   1
// b:   true   或   false ,代表对错
function kttb_setJudge(user_answer, b) {
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
	showOrHideAnalysis();
}
// 重置判断题
function kttb_resetJudge() {
	$('.button').removeClass('button_right button_error button_no button_yes button_disabled');
	$('.analysis').hide();
}
// ＊＊＊＊＊＊连续填空题＊＊＊＊＊＊＊＊＊／／
// 获取所有空的id
function kttb_getAllInput() {
	var arr = [];
	$('input').each(function(index, item) {
		arr.push($(item).attr('id'));
	})
	return arr;
}
// 上一道
function kttb_preSubject1(posi) {
	var arr = kttb_getAllInput();
	var f_id = $('input:focus').attr('id');
	if (f_id) {
		var index = arr.indexOf(f_id);
		if (index == 0) {
			window.webInteraction.notPre(posi);
		} else {
			$('input').eq(--index).trigger("click").focus();
		};
	}
}
// 下一道
function kttb_nextSubject1(posi) {
	var arr = kttb_getAllInput();
	var f_id = $('input:focus').attr('id');
	if (f_id) {
		var index = arr.indexOf(f_id);
		if (index == arr.length - 1) {
			window.webInteraction.notNext(posi);
		} else {
			$('input').eq(++index).trigger("click").focus();
		};
	}
}
// 让所有input控件失去焦点
function kttb_input_blur() {
	$('input').blur();
}
// 设置input光标位置
function kttb_setCursorPos(per) {
	var input = $('input:focus')[0];
	var length = $(input).val().length;
	var pos = Math.round(length * per);
	if (input) {
		if (navigator.userAgent.indexOf("MSIE") > -1) {
			var range = document.selection.createRange();
			var textRange = input.createTextRange();
			textRange.moveStart('character', pos);
			textRange.collapse();
			textRange.select();
		} else {
			input.setSelectionRange(pos, pos);
		}
	};
}
// 获取input光标位置
function kttb_getCursorPos(inpObj) {
	if (navigator.userAgent.indexOf("MSIE") > -1) { // IE
		var range = document.selection.createRange();
		range.text = '';
		range.setEndPoint('StartToStart', inpObj.createTextRange());
		return range.text.length;
	} else {
		return inpObj.selectionStart;
	}
}
// 获取用户答案
function kttb_getAllInputAnswer(posi) {
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
}

// 设置答案
function kttb_setAllInputAnswer(arrStr) {
	var arr = eval("(" + arrStr + ")");
	$('.fill_word .number').addClass('white');
	$(arr).each(function(index, item) {
		var id = item.id;
		var answer = item.answer;
		var b = item.isCorrect;
		$('input[id="' + id + '"]').val(answer).addClass('white').attr('readonly', true).siblings('.fill_number').addClass('none');
		if (b) {
			$('input[id="' + id + '"]').parent().addClass('bg_darkGreen no_border');
		} else {
			$('input[id="' + id + '"]').parent().addClass('bg_darkRed no_border');
		};
	});
	showOrHideAnalysis();
}

// 获取网页高度等信息
function kttb_getPosInfo() {
	var input = $('input:focus');
	var document_height = $(document).height(),
		scroll_height = $('body').scrollTop(),
		input_height = input.height(),
		input_offset_top = input.offset().top;
	var y = input_offset_top + input_height - scroll_height;
	var dpi = window.devicePixelRatio;
}

function kttb_setDivBotton(height) {
	$('.kttb_setDivBotton').remove();
	var div = $('<div>').addClass('kttb_setDivBotton');
	div.css({ 'width': '100%' });
	div.css('height', height + 'px');
	div.css('min-height', height + 'px');
	div.css('margin', '0');
	div.css('padding', '0');
	$('body').append(div);
}

function kttb_scrollTop(height) {
    $(document, "body", ".container").scrollTop(height)
}

function kttb_checkWriteAns() {
	$('.write_tip').hide();
	$('.analysis').fadeIn();
}

/**
 * 填空题答案置回
 */
function kttb_setBJInput(arrStr) {
	var arr = JSON.parse(arrStr);
	$(arr).each(function(index, item) {
		var id = item.id;
		var answer = item.answer;
		if (answer.length > 0 && answer.length < 11) {
			$('#' + id).parent().css('width', '2.985075rem');
		} else if (answer.length >= 11) {
			$('#' + id).parent().css('width', '3.880597rem');
		};
		$('#' + id).val(answer).parent().addClass('fill_word_noBgC');
	})
}

// 获取听说同步的单词内容
function kttb_getWordHtml() {
	var html = $(".word_txt").html();
	window.webInteract.getWordHtml(html);
}
// 设置听说同步的单词着色文本
function kttb_setWordHtml(html) {
	$(".word_txt").html(html);
}


// 重置gif为png
function reset_gifToPng() {
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
}

// 做题页用的重置gif为png
function newReset_gifToPng(){
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
}

// 切换网页停止播放视频音频
function stopAudioOrVideo() {
	$(".video_mask").show();
	$("audio,video").each(function(index, item) {
		if (!item.paused) {
			item.currentTime = 0;
			item.pause();
		}
	})
	var id = $('.video_img').siblings('.video_mask').attr("id");
	$('.video_img').attr("src", "../../common/images/" + id + ".png");
}

// 音标设置星;
// @param {id: 音标id，num: 星星数量(1-5)}
function phonogramSetStar(str) {
	// var o = JSON.parse(str);

	$(str).each(function(index, item) {
		var id = item.id,
			num = item.num;
		$('#' + id + " .map-cir-star").empty();
		if (num > 5 || num < 1) {
			return
		}
		for (i = 1; i <= num; i++) {
			var img = $("<img>");
			img.attr('src', "images/map-star.png");
			img.addClass("trans" + num + i);
			$('#' + id + " .map-cir-star").append(img);
		}
	});
}

// 设置某个音标气泡动画
// @param {id: 音标id}
function phonogramSetBubble(id) {
	var half_height = screen.height / 2;
	var ua = navigator.userAgent.toLowerCase();
	if (/iphone|ipad|ipod/.test(ua)) {
		/*ios*/
		half_height = half_height * devicePixelRatio;
	}
	var bubble = $("#" + id);
	console.log("javascript 1 : " + bubble);
	if (bubble && bubble.length > 0) {
		var div_half_height = $("#" + id).height() / 2;
		var to_top = $("#" + id).offset().top;
		var top = to_top - half_height + div_half_height * 4;
		$(".bubble").removeClass('bubble');
		$("#" + id).addClass('bubble');
		$("body,html,div.map-con").scrollTop(top);
	}

}

// 清除音标气泡动画
function phonogramClearBubble() {
	$(".bubble").removeClass('bubble');
}

// 设置某个音标气泡动画 (不滚动定位)
// @param {id: 音标id}
function phonogramSetBubbleOnly(id) {
	var bubble = $("#" + id);
	if (bubble && bubble.length > 0) {
		$(".bubble").removeClass('bubble');
		$("#" + id).addClass('bubble');
	}
}
// 引藏导读
function hideGuideReading(){
	$(".guide_reading").hide();
}

// 设置input内容
function simulateInputByString(str) {
	var target = $('input:focus, textarea:focus')[0];
	if(target) {
		var start = target.selectionStart;
		var end = target.selectionEnd;
		var value = target.value;
		var arr = value.split('');
		var newEnd =  end + str.length;
		if(str === '' && start === end) {
			if(end > 0) {
				arr.splice(end - 1, 1);
				newEnd--
			}
		} else {
			var len  = end - start;
			arr.splice(start, len, str);
			if(str === '') {
				newEnd = newEnd - len
			}
		}
		var newValue = arr.join('');
		console.log(newValue)
		$(target).val(newValue);
		// 设置光标位置
		target.selectionStart = newEnd;
		target.selectionEnd = newEnd;
	}
}

function goNextBlank(){
	var webviewUserAgent = navigator.userAgent.toLowerCase();
	var webviewSystem = 8;
	if (/iphone|ipad|ipod/.test(webviewUserAgent)) {
		/*ios*/
		webviewSystem = 1;
	} else if (/android/.test(webviewUserAgent)) {
		/*android*/
		webviewSystem = 2;
	}
    var dom = $('input:focus , textarea:focus').eq(0);
    var allDom = $('input, textarea');
    var i = allDom.index(dom);
    // console.log(i)
    if(i+1 >= allDom.length){
        if (webviewSystem === 2) {
			// window.webInteraction.scrollNextPage();
			if(window.webInteraction && window.webInteraction.onLastBlank) {
				window.webInteraction.onLastBlank()
			}
		} else if (webviewSystem === 1) {
            // location.href = "ets://get_rw_result?result=" + str;
        }
    }else{
        dom.blur();
		allDom.eq(i+1).focus();
        // 往上滚动20px
        // var scrollTop = $('#dxApp').scrollTop();
        // $('#dxApp').scrollTop(scrollTop+50);
    }
}


// input失去焦点
function inputBlur() {
    $("input").blur();
    $("textarea").blur();

}

/**
 * 防抖 wait毫秒内内连续触发，只执行最后一次， 500ms，2秒内触发多次，只会执行最后一次
 * @param {func} Function 执行的函数
 * @param {wait} number 间隔时间
 * @param {?immediate} bool 是否立即执行
 * @return {Function}
 */
function debounce(func, wait, immediate) {
	var wait = wait || 500
	var timer = null;
	return function () {
	  if (timer) {
		clearTimeout(timer);
		timer = null;
	  }
	  var that = this;
	  var args = arguments;
	  if (immediate) {
		var applyNow = !timer;
		timer = setTimeout(function() {
		  timer = null;
		}, wait);
		if (applyNow) {
		  func.apply(that, args);
		}
	  } else {
		timer = setTimeout(function() {
		  func.apply(that, args);
		}, wait);
	  }
	};
  }