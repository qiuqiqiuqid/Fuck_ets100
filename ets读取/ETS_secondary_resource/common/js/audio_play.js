$(function() {
    var IMAGE_BASE_URL = '';
    // icon播放
    $(document).on("click", ".my_record, .user_record", debounce(function() {
        var id = $(this).attr("id");
        if (id.indexOf('_')<0) {
            id = id + '_1'
        }
        if ($(this).parent().is(".audio_con")) {
            setAudioConBtnPlayStatus($(this))
            user_audio(id, id);
            return
        }
        etsCommon.reset_gifToPng();
        if ($(this).hasClass('playing')) {
            $(this).removeClass('playing');
        } else {
            $(this).addClass('playing');
        };
        user_audio(id, id);
    }, 500 ))


    $(document).on("click", ".stander_record", debounce(function() {
        var id = $(this).attr("id");
        if ($(this).parent().is(".audio_con")) {
            setAudioConBtnPlayStatus($(this))
            stander_audio(id);
            return
        }
        etsCommon.reset_gifToPng();
        if ($(this).hasClass('playing')) {
            $(this).removeClass('playing');
        } else {
            $(this).addClass('playing');
        };
        stander_audio(id);
    }, 500 ))

    function setAudioConBtnPlayStatus(dom) {
        var sib = dom.siblings();
        if (dom.hasClass('playing')) {
            dom.css({ 'background-color': '#fff'}).removeClass('playing');
            if (dom.hasClass('stander_record')) {
                dom.find('span').text('原音');
                dom.find('img').attr('src', IMAGE_BASE_URL+'images/ans_record_icon_blue.png');
            } else {
                dom.find('span').text('录音');
                dom.find('img').attr('src', IMAGE_BASE_URL+'images/ans_my_record_blue.png');
            };
        } else{
            dom.addClass('playing').find('span').text('停止');
            if (dom.hasClass('stander_record')) {
                if (sib.hasClass('playing')) {
                    sib.css({ 'background-color': '#fff'}).removeClass('playing');
                    sib.find('span').text('录音');
                    sib.find('img').attr('src', IMAGE_BASE_URL+'images/ans_my_record_blue.png');
                };
                dom.find('img').attr('src', IMAGE_BASE_URL+'images/ans_record_icon_onP.gif');
            } else {
                if (sib.hasClass('playing')) {
                    sib.css({ 'background-color': '#fff'}).removeClass('playing');
                    sib.find('span').text('原音');
                    sib.find('img').attr('src', IMAGE_BASE_URL+'images/ans_record_icon_blue.png');
                };
                dom.find('img').attr('src', IMAGE_BASE_URL+'images/ans_my_record_onP.gif');
            }
        }
    }

    // 多维度
    // $(document).on("touchstart", '.audio_con .stander_record,.audio_con .my_record', function(e) {
    //     $(this).css({ "background-color": '#3ad56f', "color": "#fff" });
    //     var src = $(this).find('img').attr('src').replace('.png', '_on.png');
    //     $(this).find('img').attr('src', src);
    // })
    // $(document).on("touchmove", '.audio_con .stander_record,.audio_con .my_record', function(e) {
    //     $(this).css({ 'background-color': '#fff', 'color': '#3ad56f' })
    //     var src = $(this).find('img').attr('src').replace('_on.png', '.png');
    //     $(this).find('img').attr('src', src);
    // })
    // $(document).on("touchend", '.audio_con .stander_record,.audio_con .my_record', function(e) {
    //     if ($(this).hasClass('playing')) {
    //         $(this).css({ 'background-color': '#fff', 'color': '#3ad56f' }).removeClass('playing');
    //         if ($(this).hasClass('stander_record')) {
    //             $(this).find('span').text('原音');
    //             $(this).find('img').attr('src', IMAGE_BASE_URL+'images/ans_record_icon.png');
    //         } else {
    //             $(this).find('span').text('录音');
    //             $(this).find('img').attr('src', IMAGE_BASE_URL+'images/ans_my_record.png');
    //         };
    //     } else {
    //         var sib = $(this).siblings();
    //         $(this).addClass('playing').find('span').text('停止');
    //         if ($(this).hasClass('stander_record')) {
    //             if (sib.hasClass('playing')) {
    //                 sib.css({ 'background-color': '#fff', 'color': '#3ad56f' }).removeClass('playing');
    //                 sib.find('span').text('录音');
    //                 sib.find('img').attr('src', IMAGE_BASE_URL+'images/ans_my_record.png');
    //             };
    //             $(this).find('img').attr('src', IMAGE_BASE_URL+'images/ans_record_icon_onP.gif');
    //         } else {
    //             if (sib.hasClass('playing')) {
    //                 sib.css({ 'background-color': '#fff', 'color': '#3ad56f' }).removeClass('playing');
    //                 sib.find('span').text('原音');
    //                 sib.find('img').attr('src', IMAGE_BASE_URL+'images/ans_record_icon.png');
    //             };
    //             $(this).find('img').attr('src', IMAGE_BASE_URL+'images/ans_my_record_onP.gif');
    //         };
    //     };
    // })
})