$(function() {
	var dxApp = new Vue({
		el: "#dxApp",
		data: {
            deepReloaded: false,
			system: webviewSystem,
			question: {},
			info: {},
			answerPage: false,
			userAns: '',
			standerAns: '',
			canSetAnswer: false,
			questionIndex: 1,
			errorType: 0,
		},
		mounted: function() {
			$("div.container").removeClass("hidden");
		},
		updated: function() {
			this.canSetAnswer = true;
            this.deepReloaded = true;
		},
		filters: {},
		computed: {},
		methods: {
			getInfoTag: function(tag) {
				var val = '';
				$(tag).each(function(index, item) {
					val += item + '；'
				})
				return val.substring(0, val.length - 1) + '。';
			},
			// 序列号
			calculationOrder: function(order) {
				if(this.question.type == 10){
					var info_arr = this.question.info;
					var info = this.info;
					var t_order = this.questionIndex;
					first:
					for(var i = 0; i < info_arr.length; i++){
						var sub_question_info = info_arr[i]['sub_question_info'];
						second:
						for(var j = 0; j < sub_question_info.length; j++){
							if(sub_question_info[j]['order'] != info.order){
								t_order++
							}else{
								break first
							}
						}
					}
					return t_order
				}else{
					return Number(order) + Number(this.questionIndex);
				}
				
			},
			formatInfoContent: function(val) {
				var str = replaceBlankRegCM(val);
				return str
			}
		}
	})
	window.dxApp = dxApp;

	//***** 选择题效果 *****//
	$(document).on("click", ".choose-con:not(.disabled) .choose-item", function(e) {
		e.stopPropagation();
		$(this).siblings().removeClass('active');
		$(this).addClass('active');
		getRwAnswerStr();
	})
	//***** 选择题效果结束 *****//

})