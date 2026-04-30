$(function(){
	// 点击选项
	$(document).on("click",".choose",function(e){
		$(this).addClass("choose_selected").siblings().removeClass("choose_selected");
	})
})