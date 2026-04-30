/*
function numtoa(str){
	switch(str){
		case 1:
			return 'A';
			break;
		case 2:
			return 'B';
			break;
		case 3:
			return 'C';
			break;
		case 4:
			return 'D';
			break;
		default:
			return false
	}
}*/

function numtoa(str){
  if(str){
    var numInt = parseInt(str) ;
    if(!isNaN(numInt) && numInt > 0 && numInt < 27){
      var newNumInt = numInt + "A".charCodeAt(0) - 1;
      var retVal = String.fromCharCode(newNumInt) ;
      return retVal ;
    }
  }
  return false ;
}

function setBackgroundColor(id){
	$("#"+id).addClass("question_on").siblings().removeClass("question_on");
}


function scrollTo(id){
	var h = $("#"+id).offset().top;
	$("body").scrollTop(h);
}

function setChoice(str){
	var arr = str.split(",");
	$(arr).each(function(index,item){
		if($("#"+item).hasClass("choose1")){
			$("#"+item).addClass("choose1_selected").siblings().removeClass("choose1_selected");
		}else {
			$("#"+item).addClass("choose2_selected").siblings().removeClass("choose2_selected");
		}
	});
	/*
    if ($(".choose1").length!==0) {
    	$(arr).each(function(index,item){
        	$("#"+item).addClass("choose1_selected").siblings().removeClass("choose1_selected");
    	})
    }else if ($(".choose2").length!==0) {
    	$(arr).each(function(index,item){
        	$("#"+item).addClass("choose2_selected").siblings().removeClass("choose2_selected");
    	})
    };*/
} 

function getChoice(childId){
	var arr = [];
    if ($(".choose1").length!==0) {
    	$(".choose1_selected").each(function(index,item){
    		var i = $(item).index();
    		var a = {};
    		a.id = $(this).parent().parent().parent().attr("id");
    		a.answer = numtoa(i);
    		arr.push(a);
    	})
    }
	if ($(".choose2").length!==0) {
    	$(".choose2_selected").each(function(index,item){
    		var i = $(item).index();
    		var a = {};
    		a.id = $(this).parent().parent().attr("id");
    		a.answer = numtoa(i);
    		arr.push(a);
    	})
    };
	//console.log(arr) ;
	var ua = navigator.userAgent.toLowerCase();	
	if (/iphone|ipad|ipod/.test(ua)) {
	    location.href = "ets://get_choice?arr=" + JSON.stringify(arr) + "&childId=" + childId;
	} else if (/android/.test(ua)) {
		window.webInteraction.getAnswerList(JSON.stringify(arr)+"",childId+"");
	}
}