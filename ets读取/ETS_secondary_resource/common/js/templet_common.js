$(function(){
	var pageAudio = new Audio();
	pageAudio.setAttribute('crossorigin', 'anonymous');
	var templateApp = new Vue({
		el: "#templateApp",
		data: {
			title: "",
			subtitle: "", //朗读下面一段独白，注意语音、语调、连读、辅音。
			description: "",
			dropdown_title: "",
			dropdown_subtitle: "",
			dropdown_description: "",
			hint: "",
			content: "",
			content_symbol: "",
			content_img: [],
			dropdown_con_active: false,
			materialUrl: "",
			content_choose: "",
			hint_2: "",
			hint_3: "",
			content_choose2: "",
			content_fill_answer: "",
			content_fill_answer_enable: "",
			show_topic_blank: "",
			questions: "",
			getScore: "",
			allScore: "",
			isDwdContainer: "",
			// 单词 和 听说朗读
			words: [],
			words_con: "",
			words_tla: "",
			words_mark: "",
			words_tips_tit: "",
			words_tips_src: "",
			// 朗读
			read: [],
			read_con: "",
			read_sen: "",
			read_mark: "",
			read_tla: "",
			read_title: "",
			read_titlesrc: "",
			// 音标拼读
			spell_txt_org: "",
			spell_tla: "",
			// 句子
			sentence: [],
			sen_con: "",
			sen_tla: "",
			// 短语
			phrase_con: "",
			// 短文
			essay: [],
			essay_con: "",
			essay_tla: "",
			essay_annota: "",
			essay_intro: "",
			essay_translate: false,
			// 听说同步填空
			content_gap: [],
			gap_title: "",
			gap_imgcon: "",
			gap_answer: "",
			gap_mixture: "",
			gap_comitcon: "",
			// 听说同步选择
			choose: [],
			choose_title: "",
			choose_con: "",
			choose_comitcon: "",
			// 自然拼读数据
			word_pronunciation_text: "",
			word_pronunciation_audio: "",
			word_pronunciation_audio_dom: null,
		},
		// 方法
		methods: {
			// 替换换行等为<br />
			formatString: function(str){
				return str.replace(/\r\n|\r|\n|↵/g, '<br/>')
			},
			// 点击显示或隐藏副标题以及描述等内容
			toggleDropdown: function(){
				this.dropdown_con_active =!this.dropdown_con_active
			},
			// 素材需要加上路径
			getMaterialUrl: function(file){
				return this.materialUrl + file;
			},
			// 点击显示大图
			showBigImg: function(file){
				var url = this.getMaterialUrl(file);
				etsCommon.showBigImg(url);
			},
			isPicChooseQue: function(item){
				return item['xxlist'][0]['xx_wj'] != ''
			},
			// 选择题点击事件
			chooseSelect: function(xt_index, xx_index, str){
				var xxlist = this[str]['xtlist'][xt_index]['xxlist'];
				xxlist = xxlist.map(function(item, index){
					item.selected = false;
					return item
				})
				xxlist[xx_index]['selected'] = true
				this[str]['xtlist'][xt_index]['xxlist'] = xxlist;
			},
			// 填空题事件
			fillWordInputFocus: function(dom){
				console.log(dom)
			},
			toggleAudio(event, src) {
				var target = event.target;
				console.log(event)
				if(etsCommon.system == 2) {
					// 安卓
					if($(target).hasClass('playing')) {
						etsCommon.stopWordPronunciationAudio()
						$(target).removeClass('playing')
					}else {
						this.word_pronunciation_audio_dom = target;
						etsCommon.getOssUrl(src)
					}
				}else {
					// ios
					if(pageAudio.paused) {
						// 调用客户端方法获取鉴权链接
						this.word_pronunciation_audio_dom = target;
						etsCommon.getOssUrl(src)
						// pageAudio.src = src;
						// pageAudio.currentTime = 0;
						// pageAudio.play();
						// $(target).addClass('playing')
					}else {
						pageAudio.pause();
						pageAudio.src = "";
						$(target).removeClass('playing')
					}
				}
				
			},
			playWordPronunciationAudio(src){
				var target = this.word_pronunciation_audio_dom
				pageAudio.src = src;
				pageAudio.currentTime = 0;
				pageAudio.play();
				$(target).addClass('playing')
			},
			setIconStatus(bool) {
				var target = this.word_pronunciation_audio_dom
				if(bool) {
					$(target).addClass('playing')
				}else {
					$(target).removeClass('playing')
				}
			}
		},
		// 计算属性
		computed: {

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
			$('#templateApp').removeClass('none')
			pageAudio.addEventListener('pause', function(){
				$('.playing').removeClass('playing')
			})
			pageAudio.addEventListener('ended', function(){
				$('.playing').removeClass('playing')
			})
			pageAudio.addEventListener('error', function(){
				$('.playing').removeClass('playing')
			})
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
	// etsCommon.setTemplateData(D44);
	// 输入框获取焦点时，若输入框值为空，则将父元素背景设为白色
	$(document).on('focus','.fill_word_input',function(e){
		console.log(e.target)
		if($(this).attr('readonly') !== 'readonly') {
			var val = $(this).val().trim();
			var id = $(this).attr('id');
			var arr = kttb_getAllInput();
			var index = arr.indexOf(id)+1;
			if (val=='') {
				$(this).parent().addClass('fill_word_noBgC');
			};
			// kttb_getPosInfo() ;
			var input = $(this);
			var document_height = $(document).height(),
				scroll_height = $('.container').scrollTop();
				input_height = input.height(),
				input_offset_top = input.offset().top;
			var y = input_offset_top+input_height-scroll_height;
			var dpi = window.devicePixelRatio ;
			window.webInteraction.kttb_blankFocus(index,arr.length,y,scroll_height,document_height,dpi);
			
			var obj = input[0];
			var index = kttb_getCursorPos(obj);
			window.webInteraction.kttb_getCursorPos(index,$(obj).val().length);
		}
	})
	// 输入框失去焦点时，若输入框值为空，则将父元素背景设为蓝色
	$(document).on('blur','.fill_word_input',function(){
		var val = $(this).val().trim();
		if (val=='') {
			$(this).parent().removeClass('fill_word_noBgC');
		};
	})

	// 输入框输入内容时变换宽度
	$(document).on('input','.fill_word_input',function(e){
		var val = $(this).val().trim();
		if (val.length>0&&val.length<11) {
			$(this).parent().css('width','2.985075rem');
		}else if(val.length>=11){
			$(this).parent().css('width','3.880597rem');
		};
	})

	// $(document).on('click','.fill_word_input', function(e){
	// 	// $(this).focus()
	// 	var obj = $(this)[0];
	// 	var index = kttb_getCursorPos(obj);
	// 	console.log(index)
	// 	window.webInteraction.kttb_getCursorPos(index,$(obj).val().length);
	// }) ;

	$(document).on("click",".choose1",function(e){
		$(this).addClass("choose1_selected").siblings().removeClass("choose1_selected");
	});

	$(document).on("click",".choose2",function(e){
		$(this).addClass("choose2_selected").siblings().removeClass("choose2_selected");
	});

})