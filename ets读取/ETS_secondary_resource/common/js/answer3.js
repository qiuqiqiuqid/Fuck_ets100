$(function(){
	$(document).on("click",".show_m",function(){
		$(".test_title").css("height","initial");
		$(this).removeClass("show_m").addClass("hide_m");
	})
	$(document).on("click",".hide_m",function(){
		$(".test_title").css("height","0.78125rem");
		$(this).removeClass("hide_m").addClass("show_m");
	})	
	$(document).on("click",".my_record",function(){
		var id = $(this).parent().parent().attr("id");
		var fileId = $(this).attr("id");
		user_audio(id,fileId);
	})
	$(document).on("click",".stander_record",function(){
		var id = $(this).attr("id");
		stander_audio(id);
	})
})