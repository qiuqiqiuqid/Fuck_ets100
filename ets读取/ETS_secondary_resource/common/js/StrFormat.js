String.prototype.format = function (args) {
    if (arguments.length > 0) {
        var result = this;
        if (arguments.length == 1 && typeof (args) == "object") {
            for (var key in args) {
                var reg = new RegExp("({" + key + "})", "g");
                result = result.replace(reg, args[key]);
            }
        }
        else {
            for (var i = 0; i < arguments.length; i++) {
                if (arguments[i] == undefined) {
                    return "";
                }
                else {
                    //修改了此处的表达式，之前的不能个数超过10
                    var reg = new RegExp("\\{" + i + "\\}", "g");
                    result = result.replace(reg, arguments[i]);
                }
            }
        }
        return result;
    }
    else {
        return this;
    }
}
String.prototype.num_format = function (p, index) {
    p = Number(p);  //匹配的数字
    index = Number(index);  //questionIndex
    var result = this;
    var reg = new RegExp("\\{0\\}", "g");
    result = result.replace(reg, p+index);
    return result;
}
// johnson add repalce format
String.prototype.word_voca_format = function(state, word, voca, single){
    var result = this;
    result = result.replace(/\{0\}/g, state).replace("{new_word}",word).replace("{new_vocabulary}", voca)
    if(single){
        result = result.replace("{single}",single);
    }
    return result
}

/*
 自定义替换函数
 */
String.prototype.replaceA = function (regex, new_word) {
    if (arguments.length != 2) {
        return '';
    }

    try {
        var result = this;
        var r = new RegExp(regex, "g");
        result = result.replace(r, new_word);
        return result;
    } catch (e) {
        return '';
    }
}

/*
 自定义替换函数
 */
String.prototype.replace_index = function (index, new_word) {
    if (arguments.length != 2) {
        return '';
    }
    var src_word = this;
    if (src_word.length < index) {
        return '';
    }

    try {
        var result = src_word.substring(0, index);
        result = result.concat(new_word);
        result = result.concat(src_word.substring(index + 1, src_word.length));
        return result;
    } catch (e) {
        return '';
    }
}

/*
 自定义插入函数
 */
String.prototype.insert = function (index, new_word) {
    if (arguments.length != 2) {
        return '';
    }
    var src_word = this;
    if (src_word.length < index) {
        return src_word.concat(new_word);
    }

    try {
        var result = src_word.substring(0, index);
        result = result.concat(new_word);
        result = result.concat(src_word.substring(index, src_word.length));
        return result;
    } catch (e) {
        return '';
    }
}

/**
 *  自定义 trim 函数 ie 兼容
 * */
String.prototype.trim = function () {
    return this.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
}

/*
 在文本两侧添加><
 */
String.prototype.un_trim = function () {
    try {
        var result = ">" + this + "<";
        return result;
    } catch (e) {
        return '';
    }
}

/**
 *  全角转换为半角函数
 * */
String.prototype.toHalf = function () {
    var character_dic = {
        '。': '.',
        '！': '!',
        '，': ',',
        '“': '"',
        "‘": "'",
        '（': '(',
        '）': '(',
        '￥': '$',
        '、': '.',
        '【': '[',
        '】': ']',
        '\\': '',
        '《': '<',
        '》': '>',
        '”':'"',
        '’':"'"
    }
    var str = this.replaceA('[\u4E00-\u9FFF]+', '').replaceA('&nbsp;', ' ');
    var tmp = str;
    for (var i = 0; i < str.length; i++) {
        if (character_dic[str[i]]) {
            tmp = tmp.replace_index(i, character_dic[str[i]])
        }
        var code = str.charCodeAt(i);//获取当前字符的unicode编码
        if (code >= 65281 && code <= 65373)//在这个unicode编码范围中的是所有的英文字母已经各种字符
        {
            var d = str.charCodeAt(i) - 65248;
            tmp = tmp.replace_index(i, String.fromCharCode(d));
        }
        else if (code == 12288)//空格
        {
            tmp = tmp.replace_index(i, ' ');
        }
        if (tmp[i] == '\\') {
            tmp = tmp.replace_index(i, ' ');
        }
    }
    return tmp;
}


function randomString(len) {
    len = len || 32;
    var $chars = 'ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz';
    /****默认去掉了容易混淆的字符oOLl,9gq,Vv,Uu,I1****/
    var maxPos = $chars.length;
    var pwd = '';
    for (i = 0; i < len; i++) {
        pwd += $chars.charAt(Math.floor(Math.random() * maxPos));
    }
    return pwd;
}

/*
 自定义插入函数
 */
String.prototype.parsePercent = function () {
    var src_word = this;
    if (src_word.indexOf('%') > 0) {
        return parseFloat(src_word) / 100.0;
    }
    else {
        return parseFloat(src_word);
    }
}


/*
 右键获取字符串
*/ 
String.prototype.trimForRightClick = function(){
    return this.replace(/(^\s*)|(\s*$)/g,"");
}