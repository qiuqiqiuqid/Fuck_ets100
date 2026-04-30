var vue = new Vue({
	el: "#vue",
	data: {
		set: {},//试题
	},
	computed: {
	},
	filters: {
	},
	methods: {
	}
})
window.vue = vue;

var scrollTimer;
//设置试题数据
function setData(data){
	//转对象
	if(typeof data=='string'){
		data = JSON.parse(data);
	}

	if(god==1){
		window.vue.set = mapData;
	}else{
		window.vue.set = data;
	}

	//地图背景
	if(window.vue.set.length){
		randomMap(window.vue.set[0].background);
	}else{
		randomMap(1);
	}
	
	//到达底部
	scrollTimer = setInterval(function(){
		var h = $('body').height();
		
		if(h){
			document.documentElement.scrollTop = $('body').height();
			document.body.scrollTop = $('body').height();

			clearInterval(scrollTimer);
			$('html,body').css('visibility','visible');
		}	
	},50)
}

//页面按钮****************************
//$('body').on('click','',function(){})

//返回
//$('.back-btn').on('click',function(){
//	systemFunction(function(){//ios
//
//	},function(){//安卓
//
//	})
//})

//点击关卡
$('body').on('click','.level-box',function(){
	var id = $(this).attr('data-id');
	var status = $(this).attr('data-status');

	systemFunction(function(){//ios
		location.href = "ets://map_click?setId=" + id + "&status=" + status;
	},function(){//安卓
		var obj = {"id":id,"status":status};
		var str = JSON.stringify(obj);

		window.webInteract.mapClick(str);
	})
})

//页面效果****************************
	
//随机地图
function randomMap(n){
	var r = n||1;

	$('.container').addClass('map-type-'+r);
}
//randomMap();
