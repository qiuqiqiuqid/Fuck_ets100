$(function(){
	// 触摸按钮
	$(".button:not('.button_right,.button_error')").on('touchstart',function(){
		$(this).addClass('button_hover');
	})
	$(".button:not('.button_right,.button_error')").on('touchmove',function(){
		$(this).removeClass('button_hover');
	})
	$(".button:not('.button_right,.button_error')").on('touchend',function(){
		$(this).removeClass('button_hover');
		var id = $('.container .sen').attr('id');
		var index = $(this).index();
		var x;
		if (index==0) {
			x = 'a';
		}else{
			x = 'b';
		};
		var ua = navigator.userAgent.toLowerCase();
		if (/iphone|ipad|ipod/.test(ua)) {
			location.href = "ets://get_judge?qid=" + id + "&answer=" + x;
		} else if (/android/.test(ua)) {
			window.webInteraction.getJudgeAnswer(id,x);
		}
	})
})