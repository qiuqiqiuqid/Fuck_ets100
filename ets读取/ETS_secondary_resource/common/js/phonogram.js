$(function(){
	if (screen.height<=480) {
		$(".sample_img").css({width: "3.74rem",height: "3.74rem"})
	}
	// 视频页播放视频
	var audio = $("#v_audio")[0];
	if (audio) {
		audio.addEventListener("error",a_error);	
		audio.addEventListener("ended",a_ended);	
		function a_error(){
			alert("加载音频出错");
		}
		function a_ended(){
			var id = $(audio).attr("data-id");
			var mask = $("#"+id);
			var img = mask.siblings("img");
			img.attr("src","../../common/images/"+id+".png")
			mask.show();
		}
	}
	// 视频
	var v_video = $(".v_video");
	v_video.each(function(index,item){
		if (item) {
			item.addEventListener("error",v_error);	
			item.addEventListener("ended",v_ended);
			function v_error(){
				alert("加载视频出错");
			}
			function v_ended(){
				$(item).siblings(".video_mask").show();
			}
		}
	})
	// 音标页播放音频
	// $(document).on("click",".sample_audio",function(e){
	// 	e.stopPropagation();
	// 	var id = $(this).attr("id");
	// 	reset_gifToPng();
	// 	$(this).addClass("playing");
	// 	stander_audio(id);
	// })
	$(document).on("click",".phonogram_word_item",function(e){
		e.stopPropagation();
		var that = $(this);
		var id = that.children(".sample_audio").attr("id");
		reset_gifToPng();
		that.children(".sample_audio").addClass("playing");
		if (!that.hasClass("bubble")) {
			that.addClass("bubble");
			setTimeout(function(){
				that.removeClass("bubble")
			},800)
		}else{
			return
		}
		stander_audio(id);
	})
	$(document).on("click",".sample_word_con",function(e){
		e.stopPropagation();
		var that = $(this);
		var id = that.children(".sample_audio").attr("id");
		reset_gifToPng();
		that.children(".sample_audio").addClass("playing");
		if (!that.hasClass("bubble")) {
			that.addClass("bubble");
			setTimeout(function(){
				that.removeClass("bubble")
			},800)
		}else{
			return
		}
		stander_audio(id);
	})
	$(".phonogram_word_item,.sample_word_con").on("touchstart",function(e){
		e.stopPropagation();
		$(this).removeClass("box_shadow");
	})
	$(".phonogram_word_item,.sample_word_con").on("touchmove",function(e){
		e.stopPropagation();
		$(this).addClass("box_shadow");
	})
	$(".phonogram_word_item,.sample_word_con").on("touchend",function(e){
		// e.stopPropagation();
		$(this).addClass("box_shadow");
		// var that = $(this);
		// if (!that.hasClass("bubble")) {
		// 	that.addClass("bubble");
		// 	setTimeout(function(){
		// 		that.removeClass("bubble")
		// 	},800)
		// }else{
		// 	return
		// }
		// var id = that.children(".sample_audio").attr("id");
		// reset_gifToPng();
		// that.children(".sample_audio").addClass("playing");
		// stander_audio(id);
	})


	// 视频页切换视频
	$(document).on("click","li.video_tab_item",function(e){
		e.stopPropagation();
		if ($(this).hasClass("video_tab_item_on")) {
			return
		}
		var index = $(this).index();
		$(this).addClass("video_tab_item_on").siblings().removeClass("video_tab_item_on");
		$(".sample_video_main>div").eq(index).show().siblings().hide();
		$('.video_mask').show();
		v_video.each(function(index,item){
			item.currentTime = 0;
			item.pause();
		})
		if ($(".video_img").length>0) {
			var id = $('.video_img').siblings('.video_mask').attr("id");
			$('.video_img').attr("src","../../common/images/"+id+".png");
			audio.currentTime = 0;
			audio.pause();
		}
	})
	$(document).on("click",".a_video_mask",function(e){
		e.stopPropagation();
		var id = $(this).attr("id");
		var img = $(this).siblings("img");
		$(audio).attr({"src":"../material/"+id+".mp3","data-id":id});
		$(this).hide();
		audio.play()
		img.attr("src","../material/"+id+".gif");
	})
	$(document).on("click",".v_video_mask",function(e){
		e.stopPropagation();
		$(this).siblings("video")[0].play();
		$(this).hide();
	})
})