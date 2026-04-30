$(function(){
	var templateApp = new Vue({
		el: "#templateApp",
		data: {
			title: "",
			subtitle: "", //朗读下面一段独白，注意语音、语调、连读、辅音。
			description: "",
			dropdown_title: "",
			dropdown_subtitle: "",
			dropdown_description: "",
			hint: '',
			content: "",
			content_img: "",
			dropdown_con_active: false,
			getScore: 0,
			allScore: 1,
			hasHalfStarImg: true,
			isDwdContainer: false,
			questions: [],
			containerHidden: true,
			materialUrl: '',
			content_choose: "",
			hint_2: "",
			content_choose2: "",
			content_fill_answer: "",
			// 失分单词
			phonemeInfo: {},
			// sfysMaskVisible: false,
			// sfdcMaskVisible: false,
			// yinbiaoMaskVisible: false,
			isScoreOnTop: false,
			// 复述题
			repeatTabs: ['整体评价', '要点诊断', '语言诊断'],
			repeatTabIndex: 0,
			isRepeatQuestion: false,
			isShowQuestionDetail: false,
			studentOriginText: '',
			repeatDiagnosisStatus: 0,
			repeatDiagnosisProgress: 0,
			repeatDiagnosisData: null,
			diagnosisButtonVisible: false,
			errorListAllVisible: false,
			ydfxStudentAnswerVisible: false,
			yyfxStudentAnswerVisible: false
		},
		// 方法
		methods: {
			// 复述题相关开始
			// 学生回答等首字母大写
			formatStudentAnswer: function(text, index) {
				var text = text || ''
				var index = index || 0
				var txt = text.substr(0, index)
				const firstLetter = text.charAt(index)
				if(firstLetter.toUpperCase() === firstLetter) {
					return txt + text
				}else {
					return txt + firstLetter.toUpperCase() + text.slice(index + 1)
				}
			},	
			setRepeatDiagnosisStatus: function(status) {
				this.repeatDiagnosisStatus = status || 0;
			},
			startRepeatQuestionDiagnosis: function() {
				this.repeatDiagnosisStatus = 1;
				etsCommon.startDiagnosis()
			},
			stopRepeatQuestionDiagnosis: function() {
				etsCommon.stopDiagnosis()
			},
			setRepeatQuestionDiagnosisProgress: function(progress) {
				this.repeatDiagnosisProgress = progress;
			},
			setRepeatQuestionDiagnosisData: function(data) {
				var _this = this
				if(data && Array.isArray(data)) {
					data.forEach(function(item) {
						if(item.student_answer) {
							item.student_answer = item.student_answer.replace(/#/g, "'")
						}
						if(item.grammar_error_position) {
							item.grammar_error_position = item.grammar_error_position.replace(/#/g, "'")
						}
						if(item.grammar_comment) {
							item.grammar_comment = item.grammar_comment.replace(/#/g, "'")
						}
						if(item.content_comment) {
							item.content_comment = item.content_comment.replace(/#/g, "'")
						}
					})
				}
				this.repeatDiagnosisData = data;
				if(data) {
					this.repeatDiagnosisStatus = 2;
				}
			},
			showQuestionDetail: function() {
				this.isShowQuestionDetail = !this.isShowQuestionDetail;
			},
			// 格式化学生回答文本，有错误的标红
			grammarErrorFormat: function(text, index, grammar_error_position) {
				// 找到所有匹配字符的index
				var indexList = []
				var i = text.indexOf(grammar_error_position)
				while(i!==-1) {
					indexList.push(i)
					i = text.indexOf(grammar_error_position, i+1)
				}
				// 从indexList中找到离index最近的值
				var realIndex = -1
				var diffTemp = 0
				indexList.forEach(function(i, i_index) {
					var diff = Math.abs(index - i)
					if(i_index === 0) {
						realIndex = i
						diffTemp = diff 
					} else {
						if(diff < diffTemp) {
							realIndex = i
							diffTemp = diff
						}
					}
				})
				if(realIndex > -1) {
					var error_text_html = '<span class="color_red">'+grammar_error_position+'</span>'
					var arr = text.split('')
					arr.splice(realIndex, grammar_error_position.length, error_text_html)
					text = arr.join('')
				}
				return text
			},
			formatStudentText: function(item) {
				var _this = this;
				var text = item.student_answer
				var span = '<span class="color_red">'
				if(item.grammar_score!=1) {
					var index = item.grammar_error_index
					var grammar_error_position = item.grammar_error_position
					if(grammar_error_position[0] === "'") {
						grammar_error_position = grammar_error_position.slice(1)
					}
					if(grammar_error_position[grammar_error_position.length - 1] === "'") {
						grammar_error_position = grammar_error_position.slice(0, -1)
					}
					var grammar_error_position_arr = grammar_error_position.split("', '")
					grammar_error_position_arr.forEach(function(item, index) {
						text = _this.grammarErrorFormat(text, index, item)
					})
				}
				var index = text.indexOf(span)
				if(index === 0) {
					return this.formatStudentAnswer(text, span.length)
				}else {
					return this.formatStudentAnswer(text)
				}
			},
			// 格式化语言诊断评语
			formatGrammarComment: function(comment) {
				return comment.replace(/@语法诊断：/g, '').replace(/#/g, "'");
			},
			formatContentComment: function(comment) {
				return comment.replace(/@错误诊断：/g, '').replace(/#/g, "'");
			},
			// 复述题相关结束
			setMaskModalVisible: function(key, bool) {
				// this[key] = bool;
				if(bool) {
					var type = 0;
					if(key === 'sfysMaskVisible') {
						type = 3;
					} else if(key === 'sfdcMaskVisible') {
						type = 0
					}
					etsCommon.showPopMessage({type: type})
				}
			},
			// 替换换行等为<br />
			formatString: function(str){
				return str.replace(/\r\n|\r|\n|↵/g, '<br/>')
			},
			formatHtml: function(str){
				return '<p>'+str+'</p>'
			},	
			formatHtmlPContent: function(str,audio_src,index){
				if(this.isRepeatQuestion) {
					// 
					return '<p>' + str + '</p>'
				}else {
					return '<p>' + str + (audio_src?'<span class="record_icon stander_record" id="'+audio_src+'" data-path="'+this.getMaterialPath(index)+'"></span>':'') + '</p>'
				}
			},
			formatAnswerHtmlPContent: function(str,audio_src,index){
				return '<p>· ' + str + (audio_src?'<span class="record_icon stander_record" id="'+audio_src+'" data-path="'+this.getMaterialPath(index)+'"></span>':'') + '</p>'
			},
			// 点击显示或隐藏副标题以及描述等内容
			toggleDropdown: function(){
				this.dropdown_con_active =!this.dropdown_con_active
			},
			// 素材需要加上路径
			getMaterialUrl: function(index, file){
				return this.getMaterialPath(index) + file;
			},
			getMaterialPath: function(index){
				return typeof this.materialUrl === 'string' ? this.materialUrl : this.materialUrl[index];
			},
			// 点击显示大图
			showBigImg: function(index, file){
				var url = this.getMaterialUrl(index, file);
				etsCommon.showBigImg(url);
			},
			isPicChooseQue: function(item){
				return item['xxlist'][0]['xx_wj'] != ''
			},
			getNotScoreContentAudioHtml: function(item,index){
				var html = item.st_nr;
				if (item.audio) {
					html += '<span class="record_icon stander_record" id="'+item.audio+'" data-path="'+this.getMaterialPath(index)+'"></span>'
				}
				return html
			},
			getContentAudioHtml: function(item,index){
				var html = item.st_nr;
				if (item.audio) {
					html += '<span class="red">(<span class="q_score_con">未作答</span>)</span><span class="record_icon stander_record" id="'+item.audio+'" data-path="'+this.getMaterialPath(index)+'"></span>'
				}
				return html
			},
			getXtContentAudioHtml: function(item,index){
				var html = item.xt_nr + '<span class="red">(<span class="q_score_con">未作答</span>)</span>';
				if (item.xt_wj) {
					html += '<span class="record_icon stander_record" id="'+item.xt_wj+'" data-path="'+this.getMaterialPath(index)+'"></span>'
				}
				return html
			},
			getXtValueContentAudioHtml: function(item,index){
				var html = item.xt_value || ''
				if (item.xt_wj2) {
					html += '<span class="record_icon stander_record" id="'+item.xt_wj2+'" data-path="'+this.getMaterialPath(index)+'"></span>'
				}
				return html
			}
			
		},
		// 计算属性
		computed: {
			
			// 复述题整体评价，总结评语
			repeatDiagnosisGlobalAssessment: function(){
				var data = Array.isArray(this.repeatDiagnosisData) ? this.repeatDiagnosisData : []
				var dataItem = data.find(function(item) {
					return typeof item.global_assessment !== 'undefined' && item.global_assessment !== ''
				})
				return dataItem && dataItem.global_assessment ? dataItem.global_assessment.replace(/#/g, "'") : ''
			},
			repeatDiagnosisDataErrorList: function() {
				var list = []
				var data = Array.isArray(this.repeatDiagnosisData) ? this.repeatDiagnosisData : []
				var map = {}
				data.forEach(function(item) {
					if(typeof item.grammar_score !== 'undefined' && item.grammar_score!=1) {
						if(typeof map[item.grammar_error_type] !== 'undefined') {
							list[map[item.grammar_error_type]].count++
						} else {
							map[item.grammar_error_type] = list.length
							list[list.length] = {
								error_type: item.grammar_error_type,
								count: 1,
							}
						}
					}
				})
				list.sort(function(a,b) {
					return b.count - a.count
				})
				return list.filter(function(item) {
					return item.error_type !== '学生作答未提到'
				})
			},
			hasRepeatDiagnosisDataCorrect: function() {
				return this.repeatDiagnosisData.some(function(item) {
					return item.content_score == 1
				})
			},
			hasRepeatDiagnosisDataErrorListCorrect: function() {
				return this.repeatDiagnosisDataErrorList.length > 0 && this.repeatDiagnosisData.some(function(item) {
					return item.grammar_score == 1
				})
			},
			repeatDiagnosisDataErrorMaxCount: function() {
				var list = this.repeatDiagnosisDataErrorList || []
				var max = list.length > 0 ? list[0].count : 0
				return max > 0 ? max : 1
			},
			// 复述题结束
			scoreRationColor: function(){
				var get_s = this.getScore;
				var all_s = this.allScore;
				return etsCommon.getScoreTextColor(get_s, all_s);
			},
			scoreStarRationLevel: function(){
				var ration = this.getScore / this.allScore;
				var level = 0;
				if (ration < 0.1) {
					level = 0;
				} else if (ration >= 0.1 && ration < 0.2) {
					level = 1;
				} else if (ration >= 0.2 && ration < 0.3) {
					level = 2;
				} else if (ration >= 0.3 && ration < 0.4) {
					level = 3;
				} else if (ration >= 0.4 && ration < 0.5) {
					level = 4;
				} else if (ration >= 0.5 && ration < 0.6) {
					level = 5;
				} else if (ration >= 0.6 && ration < 0.7) {
					level = 6;
				} else if (ration >= 0.7 && ration < 0.8) {
					level = 7;
				} else if (ration >= 0.8 && ration < 0.9) {
					level = 8;
				} else if (ration >= 0.9 && ration < 0.95) {
					level = 9;
				} else if (ration >= 0.95 && ration <= 1) {
					level = 10;
				};
				return level
			}
		},
		// 创造实例之前
		beforeCreate: function(){

		},
		// 创造实例之后
		created: function(){

		},
		// 挂载开始之前
		beforeMount: function(){

		},
		// 挂载到实例之后
		mounted: function(){

		},
		// 数据更新之前
		beforeUpdate: function(){

		},
		// 数据更新导致dom重新渲染之后
		updated: function(){

		},
		// 实例销毁之前
		beforeDestroy: function(){

		},
		// 实例销毁之后
		destroyed: function(){

		}
	})
	window.templateApp = templateApp;
	// 点击展示全部单词，音标等
	$(document).on('click', '.show-more', function(e) {
		e.stopPropagation();
		var dom = $(this);
		var text= dom.text();
		var scroll_content = dom.parents('.title-wrapper').siblings('.words-scroll-content');
		if(text =='展开全部') {
			dom.text('收起')
			scroll_content.addClass('visiable')
		}else {
			dom.text('展开全部')
			scroll_content.removeClass('visiable')
		}
	})

	// 点击单词弹出释义
	$(document).on('click', '.pop-word, .word-analysis .word-item', function(e) {
		etsCommon.popDismiss()
		$(this).addClass('active');
		var dom = $(this)[0];
		var text = dom.innerText.trim();
		var rect = dom.getBoundingClientRect();
		var x = rect.x;
		var y = rect.y;
		var width = rect.width;
		var height = rect.height;
		etsCommon.showWordPopup({
			word: text,
			x: x,
			y:y,
			width: width,
			height: height,
		})
	})
	// 点击音标弹窗
	$(document).on('click', '.word-analysis .phoneme-item', function(e) {
		etsCommon.popDismiss()
		$(this).addClass('active');
		var dom = $(this)[0];
		var text = dom.innerText.trim();
		var rect = dom.getBoundingClientRect();
		var x = rect.x;
		var y = rect.y;
		var width = rect.width;
		var height = rect.height;
		etsCommon.showPhonemePopup({
			word: text,
			x: x,
			y:y,
			width: width,
			height: height,
		})
	})
})