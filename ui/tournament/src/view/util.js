var m = require('mithril');
var partial = require('chessground').util.partial;

var boardContent = m('div.cg-board-wrap', m('div.cg-board'));

function miniGame(game) {
  return m('div', [
    m('a', {
      key: game.id,
      href: '/' + game.id,
      class: 'mini_board live_' + game.id + ' parse_fen is2d',
      'data-color': game.color,
      'data-fen': game.fen,
      'data-lastmove': game.lastMove,
      config: function(el, isUpdate) {
        if (!isUpdate) lichess.parseFen($(el));
      }
    }, boardContent),
    m('div.vstext.clearfix', [
      m('div.left', [
        game.user1.name,
        m('br'),
        game.user1.title ? game.user1.title + ' ' : '',
        game.user1.rating
      ]),
      m('div.right', [
        game.user2.name,
        m('br'),
        game.user2.rating,
        game.user2.title ? ' ' + game.user2.title : ''
      ])
    ])
  ]);
}

module.exports = {
  secondsFromNow: function(seconds) {
    var time = moment().add(seconds, 'seconds');
    return m('time.moment-from-now', {
      datetime: time.format()
    }, time.fromNow());
  },
  title: function(ctrl) {
    return m('h1.text[data-icon=g]', [
      ctrl.data.fullName,
      ctrl.data.private ? [
        ' ',
        m('span.text[data-icon=a]', ctrl.trans('isPrivate'))
      ] : null
    ]);
  },
  player: function(p) {
    return {
      tag: 'a',
      attrs: {
        class: 'text ulpt user_link',
        href: '/@/' + p.username
      },
      children: [
        (p.title ? p.title + ' ' : '') + p.username,
        p.rating ? ' (' + p.rating + ')' : '',
      ]
    };
  },
  games: function(games) {
    return m('div.game_list.playing', games.map(miniGame));
  }
};
