$(function(){
    var templateAppV2 = new Vue({
        el: "#templateAppV2",
        data: {
            // ========== 一期复用字段 ==========
            title: "",
            subtitle: "",
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
            isScoreOnTop: false,

            // ========== 复述题基础字段（一期复用） ==========
            isRepeatQuestion: true,
            isShowQuestionDetail: false,
            studentOriginText: '',
            repeatDiagnosisStatus: 0,
            repeatDiagnosisProgress: 0,
            diagnosisButtonVisible: false,

            // ========== 二期新增字段 ==========
            // Tab 配置
            repeatTabs: ['题目概述', '详细诊断', '作答总结'],
            repeatTabIndex: 0,

            // SSE 流式状态
            isStreaming: false,
            overviewText: '',
            overviewDone: false,
            diagnosisDone: false,
            isPartialResult: false,
            diagnosisItems: [],
            repeatDiagnosisData: null,

            // A档折叠
            correctPointsVisible: false,

            // 作答总结
            grammarAllVisible: false,

            // detectData 弹窗 (FP-13)
            detectModalVisible: false,
            detectMessage: '',

            // 错误/超时弹窗
            errorModalVisible: false,
            errorMessage: ''
        },

        computed: {
            // FP-12: 得分率区间判断 (20%, 95%) 开区间
            scoreRateInRange: function() {
                var rate = this.getScore / this.allScore;
                return rate > 0.2 && rate < 0.95;
            },

            // 一期复用: 得分颜色
            scoreRationColor: function() {
                return etsCommon.getScoreTextColor(this.getScore, this.allScore);
            },

            // 一期复用: 星级
            scoreStarRationLevel: function() {
                var ration = this.getScore / this.allScore;
                var level = 0;
                if (ration < 0.1) level = 0;
                else if (ration < 0.2) level = 1;
                else if (ration < 0.3) level = 2;
                else if (ration < 0.4) level = 3;
                else if (ration < 0.5) level = 4;
                else if (ration < 0.6) level = 5;
                else if (ration < 0.7) level = 6;
                else if (ration < 0.8) level = 7;
                else if (ration < 0.9) level = 8;
                else if (ration < 0.95) level = 9;
                else if (ration <= 1) level = 10;
                return level;
            },

            // A档判断: 是否有 level=4 的要点（A档=4分，level 为 string 类型）
            hasCorrectPoints: function() {
                return this.diagnosisItems.some(function(item) {
                    return parseInt(item.level) === 4;
                });
            },

            // FP-10: 作答总结计算（level 为 string 类型，需 parseInt）
            summaryData: function() {
                var x1 = 0, x2 = 0, x3 = 0;
                this.diagnosisItems.forEach(function(item) {
                    if (typeof item.level === 'undefined' || item.level === null || item.level === '') return;
                    var lv = parseInt(item.level);
                    if (isNaN(lv)) return;
                    if (lv === 3 || lv === 4) x1++;
                    else if (lv === 1 || lv === 2) x2++;
                    else if (lv === 0) x3++;
                });
                var x = x1 + x2 + x3;
                var y = x - x3;
                return { x1: x1, x2: x2, x3: x3, x: x, y: y };
            },

            // 语法错误汇总
            grammarSummary: function() {
                var map = {};
                var list = [];
                this.diagnosisItems.forEach(function(item) {
                    if (!item.grammar || !Array.isArray(item.grammar)) return;
                    item.grammar.forEach(function(g) {
                        if (!g.type) return;
                        if (typeof map[g.type] !== 'undefined') {
                            list[map[g.type]].count += (g.count || 1);
                        } else {
                            map[g.type] = list.length;
                            list.push({ type: g.type, count: g.count || 1 });
                        }
                    });
                });
                list.sort(function(a, b) { return b.count - a.count; });
                return list;
            },

            // 要点信息文本拼接
            summaryPointText: function() {
                var d = this.summaryData;
                if (d.x === 0) return '';
                var text = '题目总共' + d.x + '个信息要点，你的回答涵盖了' + d.y + '个要点。';
                var parts = [];
                if (d.x1 > 0) parts.push('评A-B档的要点有' + d.x1 + '个');
                if (d.x2 > 0) parts.push('评C-D档的要点有' + d.x2 + '个');
                if (d.x3 > 0) parts.push('未回答到的要点' + d.x3 + '个');
                if (parts.length > 0) {
                    text += '其中' + parts.join('，') + '。';
                }
                return text;
            },

            // 语言运用文本拼接
            summaryGrammarText: function() {
                var list = this.grammarSummary;
                if (list.length === 0) return '不存在明显错误影响要点表达，做得不错！';
                var parts = [];
                for (var i = 0; i < list.length; i++) {
                    var typeName = list[i].type || '';
                    var suffix = typeName.indexOf('错误') >= 0 ? '' : '错误';
                    parts.push(list[i].count + '个' + typeName + suffix);
                }
                return '你回答时的语言存在' + parts.join('，') + '。';
            }
        },

        methods: {
            // ========== 一期复用方法 ==========
            formatString: function(str) {
                return str.replace(/\r\n|\r|\n|↵/g, '<br/>');
            },
            formatHtml: function(str) {
                return '<p>' + str + '</p>';
            },
            formatHtmlPContent: function(str, audio_src, index) {
                return '<p>' + str + '</p>';
            },
            formatAnswerHtmlPContent: function(str, audio_src, index) {
                return '<p>· ' + str + (audio_src ? '<span class="record_icon stander_record" id="' + audio_src + '" data-path="' + this.getMaterialPath(index) + '"></span>' : '') + '</p>';
            },
            toggleDropdown: function() {
                this.dropdown_con_active = !this.dropdown_con_active;
            },
            getMaterialUrl: function(index, file) {
                return this.getMaterialPath(index) + file;
            },
            getMaterialPath: function(index) {
                return typeof this.materialUrl === 'string' ? this.materialUrl : this.materialUrl[index];
            },
            showBigImg: function(index, file) {
                var url = this.getMaterialUrl(index, file);
                etsCommon.showBigImg(url);
            },
            showQuestionDetail: function() {
                this.isShowQuestionDetail = !this.isShowQuestionDetail;
            },
            formatStudentAnswer: function(text, index) {
                var text = text || '';
                var index = index || 0;
                var txt = text.substr(0, index);
                var firstLetter = text.charAt(index);
                if (firstLetter.toUpperCase() === firstLetter) {
                    return txt + text;
                } else {
                    return txt + firstLetter.toUpperCase() + text.slice(index + 1);
                }
            },

            // ========== 二期新增方法 ==========

            // Level 数字转字母
            getLevelLetter: function(level) {
                var map = { 4: 'A', 3: 'B', 2: 'C', 1: 'D', 0: 'E' };
                return map[level] || '-';
            },

            // 格式化学生回答（v2 版: 语法错误标红）
            formatStudentAnswerV2: function(item) {
                var text = item.answer || '';
                if (item.grammar && item.grammar.length > 0) {
                    item.grammar.forEach(function(g) {
                        if (g.detail) {
                            var match = g.detail.match(/[：:]\s*(\S+)\s*[→→]/);
                            if (match && match[1]) {
                                var errorWord = match[1];
                                text = text.replace(
                                    new RegExp(errorWord.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'),
                                    '<span class="color_red">' + errorWord + '</span>'
                                );
                            }
                        }
                    });
                }
                return this.formatStudentAnswer(text);
            },

            // ========== 诊断按钮操作 ==========
            setRepeatDiagnosisStatus: function(status) {
                this.repeatDiagnosisStatus = status || 0;
            },
            startRepeatQuestionDiagnosis: function() {
                this.repeatDiagnosisStatus = 1;
                etsCommon.startDiagnosis();
            },
            stopRepeatQuestionDiagnosis: function() {
                // 只通知 Native 停止，不主动重置状态
                // Native 确认停止后会回调 onNativeDiagnosisError 或 setRepeatDiagnosisStatus(0)
                etsCommon.stopDiagnosis();
            },
            // Native 回调确认停止后调用此方法重置状态
            resetDiagnosisState: function() {
                this._stopStreaming();
                this.repeatDiagnosisStatus = 0;
                this.repeatDiagnosisProgress = 0;
                this.overviewText = '';
                this.overviewDone = false;
                this.diagnosisDone = false;
                this.isPartialResult = false;
                this.diagnosisItems = [];
                this.repeatDiagnosisData = null;
                this.repeatTabIndex = 0;
            },
            setRepeatQuestionDiagnosisProgress: function(progress) {
                this.repeatDiagnosisProgress = progress;
            },

            // ========== SSE 流式事件处理 (FP-07) ==========

            handleDiagnosisEvent: function(eventData) {
                var _this = this;

                // 首次收到事件，启动 5 分钟硬超时 (FP-14)
                if (!this.isStreaming) {
                    this.isStreaming = true;
                    this.repeatDiagnosisStatus = 1;
                    this._startTimeout();
                }

                // FP-13: detectData 审核拦截
                if (eventData.detectData) {
                    this.detectMessage = eventData.detectData.message || '内容存在违规，诊断已终止';
                    this.detectModalVisible = true;
                    this._stopStreaming();
                    return;
                }

                // 按 resType 分发
                if (eventData.resType === 1) {
                    this._handleOverview(eventData);
                } else if (eventData.resType === 2) {
                    this._handleDiagnosisRes(eventData);
                }

                // 更新进度
                if (this.diagnosisItems.length > 0) {
                    this.repeatDiagnosisProgress = Math.min(
                        Math.round(this.diagnosisItems.length * 10),
                        90
                    );
                }
            },

            // 处理 overview 事件 (resType=1)
            _handleOverview: function(eventData) {
                this._overviewBuffer[eventData.seqNo] = eventData.overview || '';
                while (this._overviewBuffer[this._overviewNextSeq] !== undefined) {
                    this.overviewText += this._overviewBuffer[this._overviewNextSeq];
                    delete this._overviewBuffer[this._overviewNextSeq];
                    this._overviewNextSeq++;
                }
            },

            // 处理 diagnosisRes 事件 (resType=2)
            _handleDiagnosisRes: function(eventData) {
                var res = eventData.diagnosisRes;
                if (!res) return;

                // 替换 # 为 '（沿用一期转义机制）
                if (res.answer) res.answer = res.answer.replace(/#/g, "'");
                if (res.pointInfo) res.pointInfo = res.pointInfo.replace(/#/g, "'");

                // grammar 解析：可能是字符串或数组，detail 中可能含 [类型]内容 需展开
                if (typeof res.grammar === 'string' && res.grammar) {
                    res.grammar = this._parseGrammarString(res.grammar);
                } else if (Array.isArray(res.grammar)) {
                    res.grammar = this._expandGrammarArray(res.grammar);
                } else {
                    res.grammar = [];
                }

                // 按 index 插入或替换（支持乱序到达）
                var existIdx = -1;
                for (var i = 0; i < this.diagnosisItems.length; i++) {
                    if (this.diagnosisItems[i].index === res.index) {
                        existIdx = i;
                        break;
                    }
                }
                if (existIdx >= 0) {
                    this.$set(this.diagnosisItems, existIdx, res);
                } else {
                    this.diagnosisItems.push(res);
                    this.diagnosisItems.sort(function(a, b) {
                        return a.index - b.index;
                    });
                }

                // 同步更新 repeatDiagnosisData 供兼容逻辑使用
                this.repeatDiagnosisData = this.diagnosisItems;
            },

            // ========== SSE 完成/错误处理 ==========

            handleDiagnosisDone: function(options) {
                var opts = options || {};
                this.overviewDone = true;
                this.diagnosisDone = true;
                this._stopStreaming();
                this.repeatDiagnosisStatus = 2;
                this.repeatDiagnosisProgress = 100;
                this.isPartialResult = !!opts.isPartial;
            },

            handleDiagnosisError: function(errorInfo) {
                this._stopStreaming();
                var info = errorInfo || {};

                // 用户主动停止：不弹窗，直接重置所有状态和已渲染内容
                if (info.errorType === 'cancel') {
                    this.resetDiagnosisState();
                    return;
                }

                // 其他错误：弹窗提示
                if (info.errorType === 'timeout') {
                    this.errorMessage = '诊断超时，请稍后重试';
                } else if (info.errorType === 'network') {
                    this.errorMessage = '网络异常，请检查网络后重试';
                } else if (info.errorType === 'server') {
                    this.errorMessage = info.message || '服务异常，请稍后重试';
                } else {
                    this.errorMessage = info.message || '诊断异常，请稍后重试';
                }
                this.errorModalVisible = true;

                // 如果已有部分数据，保留并标记完成
                if (this.diagnosisItems.length > 0) {
                    this.repeatDiagnosisStatus = 2;
                    this.diagnosisDone = true;
                } else {
                    this.repeatDiagnosisStatus = 0;
                }
            },

            // ========== 5分钟硬超时 (FP-14) ==========
            _startTimeout: function() {
                var _this = this;
                this._clearTimeout();
                this._timeoutTimer = setTimeout(function() {
                    if (_this.isStreaming) {
                        _this.handleDiagnosisError({
                            errorType: 'timeout',
                            message: '诊断超时，请稍后重试',
                            code: 408
                        });
                    }
                }, this._timeoutMs);
            },
            _clearTimeout: function() {
                if (this._timeoutTimer) {
                    clearTimeout(this._timeoutTimer);
                    this._timeoutTimer = null;
                }
            },
            _stopStreaming: function() {
                this.isStreaming = false;
                this._clearTimeout();
            },

            // 解析 grammar 字符串为数组（如 "[动词时态]xxx[主谓一致]yyy"）
            _parseGrammarString: function(grammarStr) {
                var list = [];
                var parts = grammarStr.split(/\[([^\]]+)\]/);
                for (var i = 1; i < parts.length; i += 2) {
                    var type = parts[i];
                    var detail = (i + 1 < parts.length) ? parts[i + 1].trim() : '';
                    if (type) {
                        list.push({ type: type, detail: detail, count: 1 });
                    }
                }
                return list;
            },

            // 展开 grammar 数组中 detail 含 [类型]内容 的元素
            _expandGrammarArray: function(arr) {
                var result = [];
                for (var i = 0; i < arr.length; i++) {
                    var g = arr[i];
                    var detail = g.detail || '';
                    var parsed = this._parseGrammarString(detail);
                    if (parsed.length > 0) {
                        result = result.concat(parsed);
                    } else if (detail) {
                        result.push(g);
                    }
                }
                return result;
            },

            // ========== 弹窗关闭 ==========
            closeDetectModal: function() {
                this.detectModalVisible = false;
            },
            closeErrorModal: function() {
                this.errorModalVisible = false;
            },

            // ========== 二期：加载已有诊断结果（非流式，一次性渲染） ==========
            loadRetellDiagnosisData: function(jsonData) {
                var data = typeof jsonData === 'string' ? JSON.parse(jsonData) : jsonData;
                if (!data) return;

                // 填充 overview
                this.overviewText = data.overview || '';
                this.overviewDone = true;

                // 填充 diagnosisItems
                var items = [];
                var list = data.diagnosisList || [];
                for (var i = 0; i < list.length; i++) {
                    var resList = list[i].diagnosisRes || [];
                    for (var j = 0; j < resList.length; j++) {
                        var resItem = resList[j];
                        // grammar 解析：字符串或数组，detail 中的 [类型]内容 需展开
                        if (typeof resItem.grammar === 'string' && resItem.grammar) {
                            resItem.grammar = this._parseGrammarString(resItem.grammar);
                        } else if (Array.isArray(resItem.grammar)) {
                            resItem.grammar = this._expandGrammarArray(resItem.grammar);
                        } else {
                            resItem.grammar = [];
                        }
                        items.push(resItem);
                    }
                }
                items.sort(function(a, b) { return (a.index || 0) - (b.index || 0); });
                this.diagnosisItems = items;
                this.diagnosisDone = true;
                this.repeatDiagnosisData = items;
                this.repeatDiagnosisStatus = 2;
            },

            // ========== 一期兼容: setRepeatQuestionDiagnosisData ==========
            setRepeatQuestionDiagnosisData: function(data) {
                var _this = this;
                if (data && Array.isArray(data)) {
                    data.forEach(function(item) {
                        if (item.student_answer) item.student_answer = item.student_answer.replace(/#/g, "'");
                        if (item.grammar_error_position) item.grammar_error_position = item.grammar_error_position.replace(/#/g, "'");
                        if (item.grammar_comment) item.grammar_comment = item.grammar_comment.replace(/#/g, "'");
                        if (item.content_comment) item.content_comment = item.content_comment.replace(/#/g, "'");
                    });
                }
                this.repeatDiagnosisData = data;
                // 转换为二期 diagnosisItems 格式
                if (data && Array.isArray(data)) {
                    this.diagnosisItems = data.map(function(item) {
                        return {
                            index: parseInt(item.keypoint) || 0,
                            point: item.keypointValue || '',
                            answer: item.student_answer || '',
                            pointInfo: item.content_comment
                                ? item.content_comment.replace(/@错误诊断：/g, '')
                                : (item.pointInfo || ''),
                            level: typeof item.level !== 'undefined' ? item.level
                                 : (item.content_score === 1 ? 4 : 0),
                            grammar: item.grammar || (
                                item.grammar_score !== 1 && item.grammar_error_type
                                ? [{ type: item.grammar_error_type, detail: item.grammar_comment ? item.grammar_comment.replace(/@语法诊断：/g, '') : '', count: 1 }]
                                : []
                            )
                        };
                    });
                    this.repeatDiagnosisStatus = 2;
                }
            }
        },

        created: function() {
            // Vue2 不代理下划线开头的属性，因此放在 created 中作为实例属性初始化
            this._overviewBuffer = {};
            this._overviewNextSeq = 1;
            this._diagnosisBuffer = {};
            this._timeoutTimer = null;
            this._timeoutMs = 300000;
        },
        mounted: function() {},
        beforeDestroy: function() {
            this._clearTimeout();
        }
    });

    window.templateApp = templateAppV2;

    // 复用一期的点击展示全部单词等事件委托
    $(document).on('click', '.show-more', function(e) {
        e.stopPropagation();
        var dom = $(this);
        var text = dom.text();
        var scroll_content = dom.parents('.title-wrapper').siblings('.words-scroll-content');
        if (text === '展开全部') {
            dom.text('收起');
            scroll_content.addClass('visiable');
        } else {
            dom.text('展开全部');
            scroll_content.removeClass('visiable');
        }
    });

    // 点击单词弹出释义
    $(document).on('click', '.pop-word, .word-analysis .word-item', function(e) {
        etsCommon.popDismiss();
        $(this).addClass('active');
        var dom = $(this)[0];
        var text = dom.innerText.trim();
        var rect = dom.getBoundingClientRect();
        etsCommon.showWordPopup({
            word: text,
            x: rect.x,
            y: rect.y,
            width: rect.width,
            height: rect.height
        });
    });
});
