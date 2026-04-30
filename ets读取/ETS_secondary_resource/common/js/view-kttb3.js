$(function(){
	// 输入框字体大小
	$('.write_input').on('input',function(e){
		var val = $(this).val().trim();
		if(val.length==0){
			$('.write_cancle_icon').hide();
			$(this).css({'font-size':'0.597015rem','padding-right':'0'}).parent().css('width','5.970149rem');
		}else{
			$('.write_cancle_icon').show();
			$(this).css('padding-right','0.622388rem');
			if (val.length>0&&val.length<9) {
				$(this).css('font-size','0.955224rem').parent().css('width','5.970149rem');
			}else if(val.length>=9){
				$(this).css('font-size','0.716418rem').parent().css('width','8.358209rem');
			};
		}
	})

	//清除输入框内容 
	$('.write_cancle_icon').on('touchstart',function(){
		$(this).addClass('write_cancle_icon_hover');
	})
	$('.write_cancle_icon').on('touchmove',function(){
		$(this).removeClass('write_cancle_icon_hover');
	})
	$('.write_cancle_icon').on('touchend',function(){
		$('.write_input').val('').css({'font-size':'0.597015rem','padding-right':'0'}).parent().css('width','5.970149rem');
		$('.write_cancle_icon').hide();
		$(this).removeClass('write_cancle_icon_hover');
	})

	$('.write_input').focus(function(e){
		var id = $(this).attr('id');
		var input = $(this);
		var document_height = $(document).height(),
			scroll_height = $('body').scrollTop();
			input_height = input.height(),
			input_offset_top = input.offset().top;
		var y = input_offset_top+input_height-scroll_height;
		var dpi = window.devicePixelRatio ;
		window.webInteraction.kttb_blankFocus(1,1,y,scroll_height,document_height,dpi);
	})
	// // 下一题
	// $(".next_icon:not('.next_icon_right,.next_icon_error')").on('touchstart',function(){
	// 	$(this).addClass('next_icon_hover');
	// })
	// $(".next_icon:not('.next_icon_right,.next_icon_error')").on('touchmove',function(){
	// 	$(this).removeClass('next_icon_hover');
	// })
	// $(".next_icon:not('.next_icon_right,.next_icon_error')").on('touchend',function(){
	// 	$(this).removeClass('next_icon_hover');
	// })
	// $('body').on('click',".next_icon:not('.next_icon_right,.next_icon_error')",function(e){
	// 	e.stopPropagation();
	// 	var id = $('.write_input').attr('id');
	// 	var word = $('.write_input').val().trim();
	// 	window.webInteraction.getWord(id,word);
	// })
})