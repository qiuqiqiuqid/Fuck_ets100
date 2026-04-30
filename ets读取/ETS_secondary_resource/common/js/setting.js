// $(function() {
    var province = GetQueryString('province');
    var token = GetQueryString('token');
    var titleHeight = GetQueryString('titleHeight');
    var statusHeight = GetQueryString('statusHeight');
    var arr = { true: 1, false: 0 };
    var getArr = [false, true];
    // alert(token)
    // token='eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6IjE1NTEyIiwibG9naW4iOiIxMzMxNDQxMDAwMSIsImNyZWF0ZSI6MTUzMDAxMjM4NX0.gxrLOvHcNGFrudTaTym5_MKKSlA6_mZWraoVJU0Dtj4'
    token='eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6IjExMjYiLCJsb2dpbiI6IjEzODI1MjgwMDA4IiwiY3JlYXRlIjoxNTMwMTY4Mjc2fQ.VmHl1yuy-JWPE4bynTvoSn9EhQuDgzMbX_7MGriJA8M'
    $('.time-status-bar').height(statusHeight)
    $('.head-operate-container').height(titleHeight);
    setTopColor('#ffffff');
    var VM = new Vue({
        el: '#app',
        data: {
            province: province,
            showClassInfo: true,
            showResourceInfo: true
        },
        mounted: function() {
            // this.loadData()
        },
        watch: {
            showClassInfo: function(newVal, oldVal) {
                $.ajax({
                    url: 'http://test.www.ets100.com/user/set-predict-option',
                    type: 'post',
                    data: {
                        class_show_full_name: arr[newVal],
                        client_token: token
                    },
                    success: function() {

                    },
                    error: function() {

                    }
                })
            },
            showResourceInfo: function(newVal, oldVal) {
                $.ajax({
                    url: 'http://test.www.ets100.com/user/set-predict-option',
                    type: 'post',
                    data: {
                        resource_show_full_name: arr[newVal],
                        client_token: token
                    },
                    success: function() {

                    },
                    error: function() {

                    }
                })
            }
        },
        methods: {
            loadData: function() {
                var that = this;
                $.ajax({
                    url: 'http://test.www.ets100.com/user/get-predict-option',
                    type: 'post',
                    dataType: 'json',
                    data: {
                        client_token: token
                    },
                    success: function(data) {
                        that.showClassInfo = getArr[data.data.class_show_full_name];
                        that.showResourceInfo = getArr[data.data.resource_show_full_name];

                    },
                    error:function(){
                        alert(error)
                    }
                })
            },

            // changeClass:function(){
            //  $.ajax({
            //      url:
            //  })
            // }
        }
    })
    var VM = new Vue({
        el: '#app',
        data: {
            province: province,
            showClassInfo: true,
            showResourceInfo: true
        },
        mounted: function() {
            // this.loadData()
        },
        watch: {
            showClassInfo: function(newVal, oldVal) {
                $.ajax({
                    url: 'http://test.www.ets100.com/user/set-predict-option',
                    type: 'post',
                    data: {
                        class_show_full_name: arr[newVal],
                        client_token: token
                    },
                    success: function() {

                    },
                    error: function() {

                    }
                })
            },
            showResourceInfo: function(newVal, oldVal) {
                $.ajax({
                    url: 'http://test.www.ets100.com/user/set-predict-option',
                    type: 'post',
                    data: {
                        resource_show_full_name: arr[newVal],
                        client_token: token
                    },
                    success: function() {

                    },
                    error: function() {

                    }
                })
            }
        },
        methods: {
            loadData: function() {
                var that = this;
                $.ajax({
                    url: 'http://test.www.ets100.com/user/get-predict-option',
                    type: 'post',
                    dataType: 'json',
                    data: {
                        client_token: token
                    },
                    success: function(data) {
                        that.showClassInfo = getArr[data.data.class_show_full_name];
                        that.showResourceInfo = getArr[data.data.resource_show_full_name];

                    },
                    error:function(){
                        alert(error)
                    }
                })
            },

            // changeClass:function(){
            //  $.ajax({
            //      url:
            //  })
            // }
        }
    })
    // var VM=null
    
    //   
// })