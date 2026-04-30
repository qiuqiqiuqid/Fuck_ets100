(function () {
    // 简单的 requestIdleCallback polyfill
    if (!window.requestIdleCallback) {
      try{
        window.requestIdleCallback = idleExecute
      }catch(e){
        console.log(e)
      }
    }
  
    /**
     * polyfill requestIdleCallBack
     * 当浏览器空闲时执行 避免阻塞主线程
     * 通过raf + 帧执行时间 获得一帧结束时间
     * 通过messageChannel使用宏任务让出主线程
     * @param callback
     * @param params
     * @returns
     */
    function idleExecute(callback, params) {
      const channel = new MessageChannel(); // 建立宏任务的消息通道
      const port1 = channel.port1;
      const port2 = channel.port2;
      const timeout = params?.timeout || -1;
      let cb = callback;
      let frameDeadlineTime = 0; // 当前帧结束的时间
      const frameTime = 20;
      const begin = performance.now();
      let cancelFlag = 0;
  
      const runner = (timeStamp) => {
        // 获取当前帧结束的时间
        frameDeadlineTime = timeStamp + frameTime;
        if (cb) {
          port1.postMessage("task");
        }
      };
      port2.onmessage = () => {
        const timeRemaining = () => {
          const remain = frameDeadlineTime - performance.now();
          return remain > 0 ? remain : 0;
        };
        let didTimeout = false;
        if (timeout > 0) {
          didTimeout = performance.now() - begin > timeout;
        }
        // 没有可执行的回调 直接结束
        if (!cb) {
          return;
        }
        // 当前帧没有时间&没有超时 下次再执行
        if (timeRemaining() <= 1 && !didTimeout) {
          cancelFlag = requestAnimationFrame(runner);
          return;
        }
        //有剩余时间或者超时
        cb({
          didTimeout,
          timeRemaining,
        });
        cb = null;
      };
      cancelFlag = requestAnimationFrame(runner);
      return cancelFlag;
    }
  
    window.is_hongkong_env = true//window.location.href.includes('www.ets100.com/hk');
    async function getConverter(options) {
      let converterInstance = await OpenCC.Converter(options);
      return converterInstance;
    }
  
    // 带性能优化的转换函数
    async function optimizedConvertToTraditional(options, node) {
      const converter = await getConverter(options);
      const startTime = performance.now();
  
      // 使用 requestIdleCallback 避免阻塞主线程
      await convertWithIdleCallback(converter, node);
  
      // console.log(
      //   `转换完成，耗时 ${(performance.now() - startTime).toFixed(2)}ms`
      // );
  
      // 输入框和文本域处理
      const input = document.querySelectorAll("input");
      if (input) {
        input.forEach((item) => {
          item.placeholder = converter(item.placeholder);
        });
      }
      const textarea = document.querySelectorAll("textarea");
      if (textarea) {
        textarea.forEach((item) => {
          item.placeholder = converter(item.placeholder);
        });
      }
    }
    window.optimizedConvertToTraditional = optimizedConvertToTraditional;
  
    function convertWithIdleCallback(converter, nodeTarget) {
      const walker = document.createTreeWalker(
        nodeTarget,
        NodeFilter.SHOW_TEXT,
        {
          acceptNode: function (node) {
            // if (
            //   !node.textContent.trim() ||
            //   node.parentNode.nodeName === "SCRIPT" ||
            //   node.parentNode.nodeName === "STYLE"
            // ) {
            //   return NodeFilter.FILTER_REJECT;
            // }
            return NodeFilter.FILTER_ACCEPT;
          },
        },
        false
      );
  
      return new Promise((resolve) => {
        function processNodes(deadline) {
          let nodesProcessed = 0;
          let node;
  
          while (
            (node = walker.nextNode()) &&
            (deadline.timeRemaining() > 0 || deadline.didTimeout)
          ) {
            node.textContent = converter(node.textContent);
  
            nodesProcessed++;
          }
  
          if (node) {
            // 还有节点未处理，等待下一个空闲周期
            requestIdleCallback(processNodes, { timeout: 1000 });
          } else {
            resolve();
          }
        }
  
        requestIdleCallback(processNodes, { timeout: 1000 });
      });
    }
  
    async function convertTextToHk(text) {
      const converter = await getConverter({ from: "cn", to: "hk" });
      return converter(text);
    }
  
    async function convertTextToCn(text) {
      const converter = await getConverter({ from: "hk", to: "cn" });
      return converter(text);
    }
    window.convertTextToHk = convertTextToHk;
    window.convertTextToCn = convertTextToCn;
    function convertLanguageOnce(languageFlag) {
      optimizedConvertToTraditional(
        {
          from: languageFlag === "hk" ? "cn" : "hk",
          to: languageFlag,
        },
        document.body
      );
    }
    // 选择需要观察的节点
    async function convertLanguage(languageFlag) {
      if (typeof window.MutationObserver === "undefined") {
        setTimeout(() => {
          convertLanguageOnce(languageFlag);
        }, 200)
      } else {
        convertLanguageOnce(languageFlag);
        const targetNode = document.body; // 可以替换为任何容器元素
        // 观察器的配置（需要观察哪些变动）
        const config = {
          childList: true, // 观察子节点的添加或删除
          subtree: true, // 观察所有后代节点
        };
  
        // 当观察到变动时执行的回调函数
        const callback = function (mutationsList, observer) {
          for (const mutation of mutationsList) {
            if (mutation.type === "childList") {
              // console.log("有子节点被添加或移除", languageFlag, mutation.addedNodes);
              // 处理新增的节点
              mutation.addedNodes.forEach((node) => {
                optimizedConvertToTraditional(
                  {
                    from: languageFlag === "hk" ? "cn" : "hk",
                    to: languageFlag,
                  },
                  node
                );
              });
            }
          }
        };
  
        // 创建一个观察器实例并传入回调函数
        const observer = new MutationObserver(callback);
        // 开始观察目标节点
        observer.observe(targetNode, config);
      }
    }
    window.convertLanguage = convertLanguage;
  })();
  