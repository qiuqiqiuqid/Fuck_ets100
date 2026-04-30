// 获取高度
getHeight();
setBackButtonStyle(0)
var hideJoinClass = GetQueryString('hideJoinClass');
if(hideJoinClass){
    $('.add-class, .no-score-wrapper .only').hide();
}
// 页面重新加载
$(document).on('click', '#loading-fail', function() {
    window.location.reload()
})


// $('.hava-score-container')
// })