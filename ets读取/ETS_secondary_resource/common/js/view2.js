$(function(){
	$(document).on("click",".show_m",function(){
		$(".test_title").css("height","initial");
		$(this).removeClass("show_m").addClass("hide_m");
	})
	$(document).on("click",".hide_m",function(){
		$(".test_title").css("height","0.78125rem");
		$(this).removeClass("hide_m").addClass("show_m");
	})	
	$(document).on("click",".choose1",function(e){
		$(this).addClass("choose1_selected").siblings().removeClass("choose1_selected");
	})
	$(document).on("click",".choose2",function(e){
		$(this).addClass("choose2_selected").siblings().removeClass("choose2_selected");
	})

})