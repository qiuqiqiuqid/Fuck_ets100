$(function(){
	// 输入框获取焦点时，若输入框值为空，则将父元素背景设为白色
	$('.fill_word_input').focus(function(){
		var val = $(this).val().trim();
		var id = $(this).attr('id');
		var arr = kttb_getAllInput();
		var index = arr.indexOf(id)+1;
		if (val=='') {
			$(this).parent().addClass('fill_word_noBgC');
		};
		// kttb_getPosInfo() ;
		var input = $(this);
		var document_height = $(document).height(),
			scroll_height = $('body').scrollTop();
			input_height = input.height(),
			input_offset_top = input.offset().top;
		var y = input_offset_top+input_height-scroll_height;
		var dpi = window.devicePixelRatio ;
		window.webInteraction.kttb_blankFocus(index,arr.length,y,scroll_height,document_height,dpi);
	})
	// 输入框失去焦点时，若输入框值为空，则将父元素背景设为蓝色
	$('.fill_word_input').blur(function(){
		var val = $(this).val().trim();
		if (val=='') {
			$(this).parent().removeClass('fill_word_noBgC');
		};
	})

	// 输入框输入内容时变换宽度
	$('.fill_word_input').on('input',function(e){
		var val = $(this).val().trim();
		if (val.length>0&&val.length<9) {
			$(this).parent().css('width','2.985075rem');
		}else if(val.length>=9){
			$(this).parent().css('width','3.880597rem');
		};
	})

	$('.fill_word_input').on('click',function(e){
		var obj = $(this)[0];
		var index = kttb_getCursorPos(obj);
		window.webInteraction.kttb_getCursorPos(index,$(obj).val().length);
	}) ;
})