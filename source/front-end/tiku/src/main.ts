import Vue from 'vue'
import './plugins/axios'
import App from './App.vue'
import router from './router'
import './plugins/element.js'
import Vuex from "vuex"
import "./style/theme.sass"
Vue.config.productionTip = false
// 如果在模块化构建系统中，请确保在开头调用了 Vue.use(Vuex)
Vue.use(Vuex);
const store = new Vuex.Store({
  state: {
    commits:[],
    allProblem:[],
    paperEditData:{},
    modifyPaper: [],
    title:"首页"
  },
  mutations: {
    setNewCommits (state,newcommits) {
      state.commits=newcommits
    },
    setNewProblems(state,pd){
      state.allProblem=pd;
    },
    setPaperEditData(state,paperEditData){
      state.paperEditData=paperEditData;
    },
    setModifyPaper(state,modifyPaper){
      state.modifyPaper=modifyPaper;
    },
    setTitle(state,newTitle){
      state.title=newTitle;
    }
  }
});
let me= new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app');

export default me;
