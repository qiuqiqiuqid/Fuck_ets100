$(function(){
	$(document).on("click",".show_m",function(){
		$(".test_title").css("height","initial");
		$(this).removeClass("show_m").addClass("hide_m");
	})
	$(document).on("click",".hide_m",function(){
		$(".test_title").css("height","0.78125rem");
		$(this).removeClass("hide_m").addClass("show_m");
	})	
	$(document).on("click",".record_icon",function(e){
		var id = $(this).parent().parent().attr("id");
		stander_audio(id);
	})

})
