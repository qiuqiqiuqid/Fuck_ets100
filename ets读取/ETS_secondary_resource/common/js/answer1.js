$(function(){
	//单词居中
	// function word_centerY(){
	// 	var h = $(".word_con").height();
	// 	$(".word_main").height(h).css("margin-top","-"+h/2+"px");
	// }
	// if ($(".word_main").length>0) {
	// 	word_centerY()
	// }
	// 提示和单词重叠则把提示距离顶部的距离减小
	function word_tips_offset(){
		var tipsY = $(".word_tips").offset().top;
		var tipsH = $(".word_tips").height();
		var wordY = $(".word_main").offset().top;
		if (tipsY+tipsH>wordY-10) {
			$(".word_tips").css("top","30px")
		}
	}
	if($(".word_tips").length>0){
		word_tips_offset()
	}

	$(document).on("click",".show_m",function(){
		$(".test_title").css("height","initial");
		$(this).removeClass("show_m").addClass("hide_m");
	})
	$(document).on("click",".hide_m",function(){
		$(".test_title").css("height","0.78125rem");
		$(this).removeClass("hide_m").addClass("show_m");
	})	
	// icon播放
	$(document).on("click",".my_record",function(){
		var id = $(this).parent().parent().attr("id");
		if ($(this).parent().is(".audio_con")) {
			var fileId = $(this).attr("id");
			user_audio(id,fileId);
			return
		}
		reset_gifToPng();
		if ($(this).is('img')) {
			var fileId = $(this).attr("id");
			if ($(this).hasClass('playing')) {
				$(this).removeClass('playing').attr('src','../../../common/images/ans_my_record.png');
			}else{
				$(this).addClass('playing').attr('src','../../../common/images/ans_my_record.gif');
			};
		}else{
			var fileId = $(this).find('img').attr("id");
			if ($(this).hasClass('playing')) {
				$(this).removeClass('playing').find('img').attr('src','../../../common/images/ans_my_record.png');
			}else{
				$(this).addClass('playing').find('img').attr('src','../../../common/images/ans_my_record.gif');
			};
		}
		user_audio(id,fileId);
	})
	$(document).on("click",".stander_record",function(){
		if ($(this).parent().is(".audio_con")) {
			var id = $(this).attr("id");
			stander_audio(id);
			return
		}
		reset_gifToPng();
		if ($(this).is('img')) {
			var id = $(this).attr("id");
			var play_id = $(this).attr('data-id');
			if(play_id){
				if($(this).hasClass('playing')) {
					$(this).removeClass('playing').attr('src','images/ans_record_icon.png');
				}else{
					$(this).addClass('playing').attr('src','images/ans_record_icon.gif');
				};
			}else if($(this).hasClass('playing')) {
				$(this).removeClass('playing').attr('src','../../../common/images/ans_record_icon.png');
			}else{
				$(this).addClass('playing').attr('src','../../../common/images/ans_record_icon.gif');
			};
		}else{
			var id = $(this).find('img').attr("id");
			if ($(this).hasClass('playing')) {
				$(this).removeClass('playing').find('img').attr('src','../../../common/images/ans_record_icon.png');
			}else{
				$(this).addClass('playing').find('img').attr('src','../../../common/images/ans_record_icon.gif');
			};
		}
		
		stander_audio(id);
	})


	$(document).on("touchstart",'.audio_con .stander_record,.audio_con .my_record',function(e){
		$(this).css({"background-color":'#3ad56f',"color":"#fff"});
		var src = $(this).find('img').attr('src').replace('.png','_on.png');
		$(this).find('img').attr('src',src);
	})
	$(document).on("touchmove",'.audio_con .stander_record,.audio_con .my_record',function(e){
		$(this).css({'background-color':'#fff','color':'#3ad56f'})
		var src = $(this).find('img').attr('src').replace('_on.png','.png');
		$(this).find('img').attr('src',src);
	})
	$(document).on("touchend",'.audio_con .stander_record,.audio_con .my_record',function(e){
		if ($(this).hasClass('playing')) {
			$(this).css({'background-color':'#fff','color':'#3ad56f'}).removeClass('playing');
			if ($(this).hasClass('stander_record')) {
				$(this).find('span').text('原音');
				$(this).find('img').attr('src','../../../common/images/ans_record_icon.png');
			}else{
				$(this).find('span').text('录音');
				$(this).find('img').attr('src','../../../common/images/ans_my_record.png');
			};
		}else{
			var sib = $(this).siblings();
			$(this).addClass('playing').find('span').text('停止');
			var id = $('.question3').attr('id');
			var fileId = $(this).attr("id");
			if ($(this).hasClass('stander_record')) {
				if (sib.hasClass('playing')) {
					sib.css({'background-color':'#fff','color':'#3ad56f'}).removeClass('playing');
					sib.find('span').text('录音');
					sib.find('img').attr('src','../../../common/images/ans_my_record.png');
				};
				$(this).find('img').attr('src','../../../common/images/ans_record_icon_onP.gif');
			}else{
				if (sib.hasClass('playing')) {
					sib.css({'background-color':'#fff','color':'#3ad56f'}).removeClass('playing');
					sib.find('span').text('原音');
					sib.find('img').attr('src','../../../common/images/ans_record_icon.png');
				};
				$(this).find('img').attr('src','../../../common/images/ans_my_record_onP.gif');
			};
		};
	})

	$(".audio_con .stander_record").each(function(index,item){
		if($(item).attr('id') == ''){
			$(item).remove();
		}
	})
})