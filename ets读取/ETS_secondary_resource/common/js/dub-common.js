//这是小学app default js *********************************************
//全局变量
var path = window.location.pathname;
var test = getPar('test');//是否测试环境
var god = getPar('god');//是否上帝模式
var token = getPar('token');
var system = 8;//当前系统
var get_system = getPar('system');
var isHuaweiPhone = false;//是否华为
var ua = navigator.userAgent.toLowerCase();
if(test==1){
	var url = 'http://test.www.ets100.com/ets-pri';
}else{
	var url = 'http://www.ets100.com/ets-pri';
}
//判断系统 ios 1 安卓 2 web 8
if (get_system){
	//设定system
	system = get_system;
} else if (/ets100\.ios\.app/.test(ua)) {
	/*ios*/
	system = 1;
} else if (/ets100\.android\.app/.test(ua)) {
	/*android*/
	system = 2;
	try {
		isHuaweiPhone = window.webInteract.isHuaweiPhone();
	} catch (err) {
		isHuaweiPhone = false;
	}
}
//根据不同系统调用方法
function systemFunction(a,b){
	if(system==1){//ios
		a();
	}else if(system==2){//安卓
		b();
	}else{
		showTips('web上没有方法');
	}
}
//返回正确页面
function returnToCurrentPage() {
    var res = 0;
    
	goBackUrl();

    return res
}
//回退url
function goBackUrl() {
    if (path.match('/primary-app-common/member-center.html')) {//会员中心
		window.webInteract.closeWebView();
    } else if (path.match('/primary-app-common/member-pay.html')) {//会员购买页
		goUrl('member-center.html');
    }
}
//跳转到url 带当前页面参数
function goUrl(page){
	window.location.href = page + '?' + window.location.href.split('?')[1];
}
//获取 地址参数
function getPar(str) {
    var href = String(window.document.location.href);
    var rs = new RegExp("([\?&])(" + str + ")=([^&]*)(&|$)", "gi").exec(href);
    if (rs) {
        return decodeURI(rs[3]);
    } else {
        return '';
    }
}
//返回日期的函数
Date.prototype.Add = function(interval, number) {
	switch (interval) {
		case 's': return new Date(Date.parse(this) + (1000 * number));
		case 'm': return new Date(Date.parse(this) + (60000 * number));
		case 'h': return new Date(Date.parse(this) + (3600000 * number));
		case 'd': return new Date(Date.parse(this) + (86400000 * number));
		case 'w': return new Date(Date.parse(this) + ((86400000 * 7) * number));
		case 'M': return new Date(this.getFullYear(), (this.getMonth()) + number, this.getDate());
		case 'q': return new Date(this.getFullYear(), (this.getMonth()) + number * 3, this.getDate());
		case 'y': return new Date((this.getFullYear() + number), this.getMonth(), this.getDate());
	}
}
Date.prototype.AddSecond = function(number) { return this.Add("s", number); }
Date.prototype.AddMinute = function(number) { return this.Add("m", number); }
Date.prototype.AddHour = function(number) { return this.Add("h", number); }
Date.prototype.AddDay = function(number) { return this.Add("d", number); }
Date.prototype.AddWeek = function(number) { return this.Add("w", number); }
Date.prototype.AddMonth = function(number) { return this.Add("M", number); }
Date.prototype.AddQuarter = function(number) { return this.Add("q", number); }
Date.prototype.AddYear = function(number) { return this.Add("y", number); }
//日期转换
function changeDate(d){
	var d = d||new Date();
	
	var y = d.getFullYear();
	var m = addZero(d.getMonth()+1);
	var dd = addZero(d.getDate());
	var h = addZero(d.getHours());
	var mm = addZero(d.getMinutes());

	var text = y + '-' + m + '-' + dd + ' ' + h + ':' + mm;
	return text
}
//单位数前面补0
function addZero(t){
	return String(t).length==1?0+String(t):t
}
//显示提示
function showTips(str,time,callback){
    var t = time || 2000;
    if ($('.tips-con').length==0) {
        var div = $("<div>").addClass("tips-con");
        $('body').append(div);
    };
    $(".tips-con").text(str);
    var width = $('.tips-con').innerWidth()/2;
    $('.tips-con').css("margin-left",-width+'px').show();
    setTimeout(function(){
        $('.tips-con').text('').hide();
            if (callback) {
                callback()
            };
    },t)
}
//English
var English = 'ABCDEFGHIJKLMNOPQ';
//nostyle 过滤整合
function nostyle(val){
	//过滤样式
	val = val.replace(/{{underline}}/g,"<u>")
			.replace(/{{\/underline}}/g, "</u>")
			.replace(/{{italic}}/g,"<i>")
			.replace(/{{\/italic}}/g, "</i>")
			.replace(/{{bold}}/g,"<b>")
			.replace(/{{\/bold}}/g, "</b>")
			.replace(/{{center}}/g,"<c>")
			.replace(/{{\/center}}/g, "</c>")
			.replace(/color:/g,'')
			.replace(/font-size:/g,'')
			.replace(/font-family:/g,'')
			.replace(/line-height:/g,'')
			.replace(/margin:/g,'')
			.replace(/padding:/g,'')
			.replace(/\r\n/g,'<br>')
			.replace(/\r/g,'<br>')
			.replace(/\n/g,'<br>')
			.replace(/↵/g,'<br>')

	return val
}
//阅读回答 过滤整合
function noSymbol(a){
	//过滤标点
	a = a.replace(/\./g,'')
			.replace(/\,/g,'')
			.replace(/\:/g,'')
			.replace(/\;/g,'')
			.replace(/\'/g,'')
			.replace(/\"/g,'')
			.replace(/\。/g,'')
			.replace(/\，/g,'')
			.replace(/\：/g,'')
			.replace(/\；/g,'')
			.replace(/\‘/g,'')
			.replace(/\“/g,'')
			.replace(/\’/g,'')
			.replace(/\”/g,'')
			.replace('\n','')
			.trim();

	return a
}

//default 方法
//关闭窗口
$('body').on('click','.icon-close',function(e){
	e.stopPropagation();

	$(this).parent().hide();
	$('.mask').hide();

	$('html,body').remove('overHidden');
})

//if-cancel-btn
$('body').on('click','.if-cancel-btn',function(e){
	e.stopPropagation();

	$('.mask,.if-win').hide();

	$('html,body').remove('overHidden');
})

//关闭icon touch
$('body').on('touchstart','.icon-close',function(){
	$(this).addClass('icon-close-t');
}).on('touchend','.icon-close',function(){
	$(this).removeClass('icon-close-t');
})

//默认 btn touch
$('body').on('touchstart','.default-btn',function(){
	$(this).addClass('default-btn-t');
}).on('touchend','.default-btn',function(){
	$(this).removeClass('default-btn-t');
})

//默认 btn touch
$('body').on('touchstart','.default-btn-m',function(){
	$(this).addClass('default-btn-m-t');
}).on('touchend','.default-btn-m',function(){
	$(this).removeClass('default-btn-m-t');
})

//默认 btn touch
$('body').on('touchstart','.see-answer-btn',function(){
	$(this).addClass('see-answer-btn-t');
}).on('touchend','.see-answer-btn',function(){
	$(this).removeClass('see-answer-btn-t');
})

//默认 btn touch
$('body').on('touchstart','.see-answer-btn-m',function(){
	$(this).addClass('see-answer-btn-m-t');
}).on('touchend','.see-answer-btn-m',function(){
	$(this).removeClass('see-answer-btn-m-t');
})

//if-win touch
$('body').on('touchstart','.if-btn',function(){
	$(this).addClass('if-btn-t');
}).on('touchend','.if-btn',function(){
	$(this).removeClass('if-btn-t');
})